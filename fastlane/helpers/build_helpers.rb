require "base64"
require "fileutils"
require "tmpdir"

class BuildHelpers
  SIGNING_ENV_VARS = %w[KEYSTORE KEYSTORE_PASSWORD KEYSTORE_ALIAS KEYSTORE_ALIAS_PASSWORD].freeze
  SIGNING_PROPERTIES = "signing.properties".freeze

  def self.validate_pr_number(pr_number)
    raise "PR_NUMBER is required for the pr lane" unless pr_number && !pr_number.to_s.empty?
    pr_number.to_s
  end

  def self.assert_single_apk(expected_dir, pattern)
    apks = Dir.glob(File.join(expected_dir, pattern))
    raise "Expected exactly one #{pattern}, found #{apks.size}" unless apks.size == 1
    apks.first
  end

  # Decodes the keystore into +tmpdir+ and writes a signing.properties file that
  # Gradle can consume from +project_root+. Returns the path of the written
  # signing.properties so the caller can delete it. Fails closed: every signing
  # environment variable is required, and no secret is ever written outside of
  # +tmpdir+ (apart from the throwaway signing.properties Gradle needs).
  def self.setup_signing(tmpdir, project_root)
    missing = missing_signing_env_vars
    unless missing.empty?
      raise "Release signing is not configured: missing environment " \
            "variable(s) #{missing.join(', ')}"
    end

    keystore = File.join(tmpdir, "keystore.jks")
    File.binwrite(keystore, decode_keystore(ENV.fetch("KEYSTORE")))
    File.chmod(0o600, keystore)

    props = File.join(tmpdir, SIGNING_PROPERTIES)
    File.write(props, signing_properties_body(keystore))
    File.chmod(0o600, props)

    dest = File.join(project_root, SIGNING_PROPERTIES)
    FileUtils.cp(props, dest)
    File.chmod(0o600, dest)
    dest
  end

  def self.missing_signing_env_vars
    SIGNING_ENV_VARS.reject { |name| ENV[name] && !ENV[name].strip.empty? }
  end

  def self.decode_keystore(encoded)
    # GitHub secrets frequently keep the trailing newline of `base64 keystore.jks`.
    Base64.strict_decode64(encoded.gsub(/\s+/, ""))
  rescue ArgumentError
    raise "KEYSTORE is not valid base64 content"
  end

  def self.signing_properties_body(keystore)
    lines = {
      "storeFile" => keystore,
      "storePassword" => ENV.fetch("KEYSTORE_PASSWORD"),
      "keyAlias" => ENV.fetch("KEYSTORE_ALIAS"),
      "keyPassword" => ENV.fetch("KEYSTORE_ALIAS_PASSWORD")
    }
    "#{lines.map { |key, value| "#{key}=#{escape_property(value)}" }.join("\n")}\n"
  end

  # Escapes a value for java.util.Properties, which is how Gradle loads the file.
  def self.escape_property(value)
    if value.include?("\n") || value.include?("\r")
      raise "Signing property values must not contain newlines"
    end

    value.gsub(/[\\=:#!]/) { |char| "\\#{char}" }
  end

  def self.extract_version_name(gradle_file_path = "app/build.gradle.kts")
    content = File.read(gradle_file_path)
    match = content.match(/versionName\s*=\s*["']([^"']+)["']/)
    raise "versionName not found in #{gradle_file_path}" unless match
    match[1]
  end
end
