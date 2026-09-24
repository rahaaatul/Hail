require "open3"
require "digest/sha2"

class GithubReleaseHelper
  def self.publish(tag:, repository:, api_token:, name:, description:, asset_path:, prerelease: false)
    raise "tag is required" unless tag
    existing = Fastlane::Actions.run(:get_github_release,
      repository: repository,
      api_token: api_token,
      tag: tag
    )
    if existing
      raise "Release #{tag} exists with mismatched provenance" unless digest_matches?(existing, asset_path, api_token: api_token)
      existing
    else
      Fastlane::Actions.run(:set_github_release,
        repository: repository,
        api_token: api_token,
        tag: tag,
        name: "v#{name}",
        description: description,
        upload_assets: [asset_path],
        prerelease: prerelease
      )
    end
  end

  def self.digest_matches?(release, asset_path, api_token: nil)
    local_name = File.basename(asset_path)
    local_size = File.size(asset_path)
    expected = "sha256:#{Digest::SHA256.hexdigest(File.binread(asset_path))}"

    assets = release["assets"] || []
    matched = assets.find { |a| a["name"] == local_name }
    return false unless matched

    if matched["size"] && matched["size"] != local_size
      return false
    end

    return matched["digest"] == expected if matched["digest"]

    download_url = matched["url"]
    raise "Asset #{local_name} has no download URL" unless download_url

    response = Fastlane::Actions.run(:github_api,
      url: download_url,
      api_token: api_token,
      http_method: "GET",
      headers: { "Accept" => "application/octet-stream" },
      error_handlers: {}
    )

    if response[:status] == 404
      raise "Asset #{local_name} download returned 404 - asset is missing"
    end

    unless response[:status] == 200
      raise "GitHub API returned status #{response[:status]} (#{response[:body]})"
    end

    downloaded = "sha256:#{Digest::SHA256.hexdigest(response[:body])}"
    downloaded == expected
  end
end
