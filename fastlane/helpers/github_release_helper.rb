class GithubReleaseHelper
  def self.publish(tag:, repository:, api_token:, name:, description:, asset_path:, prerelease: false)
    raise "tag is required" unless tag
    existing = Fastlane::Actions.run(:get_github_release, repository: repository, tag: tag)
    if existing
      raise "Release #{tag} exists with mismatched provenance" unless digest_matches?(existing, asset_path)
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

  def self.digest_matches?(release, asset_path)
    local_size = File.size(asset_path)
    local_name = File.basename(asset_path)
    assets = release["assets"] || []
    matched = assets.find { |a| a["name"] == local_name }
    return false unless matched
    matched["size"] == local_size
  end
end
