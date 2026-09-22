require "minitest/autorun"
require_relative "../helpers/github_release_helper"

class GithubReleaseHelperSpec < Minitest::Test
  def test_missing_tag_raises
    error = assert_raises(RuntimeError) do
      GithubReleaseHelper.publish(tag: nil, name: "", description: "", asset_path: "", repository: "owner/repo", api_token: "token")
    end
    assert_includes(error.message, "tag")
  end
end
