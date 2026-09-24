require "minitest/autorun"
require "minitest/mock"
require "tmpdir"
require "digest"
require "fastlane"
require_relative "../helpers/github_release_helper"

class GithubReleaseHelperSpec < Minitest::Test
  def test_missing_tag_raises
    error = assert_raises(RuntimeError) do
      GithubReleaseHelper.publish(tag: nil, name: "", description: "", asset_path: "", repository: "owner/repo", api_token: "token")
    end
    assert_includes(error.message, "tag")
  end

  def test_matching_asset_digest
    with_temp_artifact("matching bytes") do |asset_path|
      release = release_with_asset(
        "name" => "app.apk",
        "digest" => "sha256:#{Digest::SHA256.hexdigest("matching bytes")}"
      )

      assert GithubReleaseHelper.digest_matches?(release, asset_path)
    end
  end

  def test_mismatching_asset_digest
    with_temp_artifact("local bytes") do |asset_path|
      release = release_with_asset(
        "name" => "app.apk",
        "digest" => "sha256:#{Digest::SHA256.hexdigest("remote bytes")}"
      )

      refute GithubReleaseHelper.digest_matches?(release, asset_path)
    end
  end

  def test_same_name_and_size_with_different_bytes
    local_bytes = "local-content!"
    remote_bytes = "remote-content"
    assert_equal local_bytes.bytesize, remote_bytes.bytesize

    with_temp_artifact(local_bytes) do |asset_path|
      release = release_with_asset(
        "name" => "app.apk",
        "size" => local_bytes.bytesize,
        "digest" => "sha256:#{Digest::SHA256.hexdigest(remote_bytes)}"
      )

      refute GithubReleaseHelper.digest_matches?(release, asset_path)
    end
  end

  def test_missing_asset
    with_temp_artifact("local bytes") do |asset_path|
      release = { "assets" => [{ "name" => "other.apk", "digest" => "sha256:#{Digest::SHA256.hexdigest("local bytes")}" }] }

      refute GithubReleaseHelper.digest_matches?(release, asset_path)
    end
  end

  def test_downloads_and_hashes_asset_when_digest_is_absent
    with_temp_artifact("downloaded bytes") do |asset_path|
      release = release_with_asset(
        "name" => "app.apk",
        "url" => "https://api.github.test/assets/1"
      )
      calls = []

      Fastlane::Actions.stub(:run, lambda do |action, **options|
        calls << [action, options]
        { status: 200, body: "downloaded bytes" }
      end) do
        assert GithubReleaseHelper.digest_matches?(release, asset_path, api_token: "token")
      end

      assert_equal 1, calls.length
      action, options = calls.first
      assert_equal :github_api, action
      assert_equal "https://api.github.test/assets/1", options[:url]
      assert_equal "token", options[:api_token]
      assert_equal "GET", options[:http_method]
      assert_equal({ "Accept" => "application/octet-stream" }, options[:headers])
      assert_instance_of Hash, options[:error_handlers]
    end
  end

  def test_downloaded_asset_with_different_bytes
    with_temp_artifact("local bytes") do |asset_path|
      release = release_with_asset("name" => "app.apk", "url" => "https://api.github.test/assets/1")

      Fastlane::Actions.stub(:run, lambda do |_action, **_options|
        { status: 200, body: "different bytes" }
      end) do
        refute GithubReleaseHelper.digest_matches?(release, asset_path, api_token: "token")
      end
    end
  end

  def test_missing_asset_download_url_raises
    with_temp_artifact("local bytes") do |asset_path|
      release = release_with_asset("name" => "app.apk")

      error = assert_raises(RuntimeError) do
        GithubReleaseHelper.digest_matches?(release, asset_path, api_token: "token")
      end

      assert_includes(error.message, "download URL")
    end
  end

  def test_asset_download_404_raises_explicitly
    with_temp_artifact("local bytes") do |asset_path|
      release = release_with_asset("name" => "app.apk", "url" => "https://api.github.test/assets/1")

      Fastlane::Actions.stub(:run, lambda do |_action, **_options|
        { status: 404, body: "not found" }
      end) do
        error = assert_raises(RuntimeError) do
          GithubReleaseHelper.digest_matches?(release, asset_path, api_token: "token")
        end
        assert_includes(error.message, "missing")
      end
    end
  end

  def test_asset_download_api_error_raises_explicitly
    with_temp_artifact("local bytes") do |asset_path|
      release = release_with_asset("name" => "app.apk", "url" => "https://api.github.test/assets/1")

      Fastlane::Actions.stub(:run, lambda do |_action, **_options|
        { status: 503, body: "service unavailable" }
      end) do
        error = assert_raises(RuntimeError) do
          GithubReleaseHelper.digest_matches?(release, asset_path, api_token: "token")
        end
        assert_includes(error.message, "GitHub API")
        assert_includes(error.message, "503")
      end
    end
  end

  private

  def release_with_asset(asset)
    { "assets" => [asset] }
  end

  def with_temp_artifact(contents)
    Dir.mktmpdir do |dir|
      path = File.join(dir, "app.apk")
      File.open(path, "wb") { |f| f.binmode; f.write(contents) }
      yield path
    end
  end
end
