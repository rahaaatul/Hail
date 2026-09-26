require "base64"
require "minitest/autorun"
require "tmpdir"
require_relative "../helpers/build_helpers"

class BuildHelpersSpec < Minitest::Test
  SIGNING_KEYSTORE = Base64.strict_encode64("fake-keystore-bytes").freeze

  def setup
    @saved_env = {}
    BuildHelpers::SIGNING_ENV_VARS.each { |name| @saved_env[name] = ENV[name] }
    @saved_env["KEYSTORE"] = nil
    @tmpdir = Dir.mktmpdir("build-helpers-spec-")
    @project_root = Dir.mktmpdir("build-helpers-project-")
  end

  def teardown
    @saved_env.each { |name, value| value.nil? ? ENV.delete(name) : ENV[name] = value }
    FileUtils.rm_rf(@tmpdir)
    FileUtils.rm_rf(@project_root)
  end

  def with_signing_env(overrides = {})
    BuildHelpers::SIGNING_ENV_VARS.each do |name|
      ENV[name] = overrides.fetch(name) { SIGNING_ENV_VARS_DEFAULT.fetch(name) }
    end
  end

  SIGNING_ENV_VARS_DEFAULT = {
    "KEYSTORE" => SIGNING_KEYSTORE,
    "KEYSTORE_PASSWORD" => "store-secret",
    "KEYSTORE_ALIAS" => "hail",
    "KEYSTORE_ALIAS_PASSWORD" => "key-secret"
  }.freeze

  def test_missing_pr_number_raises
    error = assert_raises(RuntimeError) do
      BuildHelpers.validate_pr_number(nil)
    end
    assert_includes(error.message, "PR_NUMBER")
  end

  def test_setup_signing_writes_properties_for_gradle
    with_signing_env
    dest = BuildHelpers.setup_signing(@tmpdir, @project_root)

    assert_equal File.join(@project_root, "signing.properties"), dest
    assert File.file?(dest)
    assert_equal "0600", format("%04o", File.stat(dest).mode & 0o777)

    keystore = File.join(@tmpdir, "keystore.jks")
    assert_equal SIGNING_KEYSTORE, Base64.strict_encode64(File.binread(keystore))
    assert_equal "0600", format("%04o", File.stat(keystore).mode & 0o777)

    body = File.read(dest)
    assert_includes body, "storeFile=#{keystore}\n"
    assert_includes body, "storePassword=store-secret\n"
    assert_includes body, "keyAlias=hail\n"
    assert_includes body, "keyPassword=key-secret\n"
  end

  def test_setup_signing_accepts_wrapped_base64
    with_signing_env("KEYSTORE" => "#{SIGNING_KEYSTORE}\n")
    BuildHelpers.setup_signing(@tmpdir, @project_root)
    assert_equal "fake-keystore-bytes",
                 File.binread(File.join(@tmpdir, "keystore.jks"))
  end

  def test_setup_signing_fails_closed_for_each_missing_variable
    BuildHelpers::SIGNING_ENV_VARS.each do |name|
      with_signing_env(name => "")
      error = assert_raises(RuntimeError) do
        BuildHelpers.setup_signing(@tmpdir, @project_root)
      end
      assert_includes error.message, name
      refute File.exist?(File.join(@project_root, "signing.properties"))
    end
  end

  def test_setup_signing_rejects_invalid_base64
    with_signing_env("KEYSTORE" => "not base64!!")
    error = assert_raises(RuntimeError) { BuildHelpers.setup_signing(@tmpdir, @project_root) }
    assert_includes error.message, "base64"
    refute File.exist?(File.join(@project_root, "signing.properties"))
  end

  def test_setup_signing_escapes_properties_syntax
    with_signing_env("KEYSTORE_PASSWORD" => "a=b\\c:d#e")
    dest = BuildHelpers.setup_signing(@tmpdir, @project_root)
    assert_includes File.read(dest), "storePassword=a\\=b\\\\c\\:d\\#e\n"
  end

  def test_setup_signing_rejects_newlines_in_values
    with_signing_env("KEYSTORE_ALIAS" => "hail\nother")
    error = assert_raises(RuntimeError) { BuildHelpers.setup_signing(@tmpdir, @project_root) }
    assert_includes error.message, "newline"
  end
end
