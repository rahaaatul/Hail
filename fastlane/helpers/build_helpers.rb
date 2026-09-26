class BuildHelpers
  def self.validate_pr_number(pr_number)
    raise "PR_NUMBER is required for the pr lane" unless pr_number && !pr_number.to_s.empty?
    pr_number.to_s
  end

  def self.assert_single_apk(expected_dir, pattern)
    apks = Dir.glob(File.join(expected_dir, pattern))
    raise "Expected exactly one #{pattern}, found #{apks.size}" unless apks.size == 1
    apks.first
  end

  def self.setup_signing(tmpdir)
    if ENV["KEYSTORE"]
      keystore = File.join(tmpdir, "keystore.jks")
      File.binwrite(keystore, Base64.strict_decode64(ENV["KEYSTORE"]))
      props = File.join(tmpdir, "signing.properties")
      File.write(props, "storeFile=#{keystore}\n" \
        "storePassword=#{ENV["KEYSTORE_PASSWORD"]}\n" \
        "keyAlias=#{ENV["KEYSTORE_ALIAS"]}\n" \
        "keyPassword=#{ENV["KEYSTORE_ALIAS_PASSWORD"]}\n")
      props
    elsif File.file?("signing.properties")
      "signing.properties"
    else
      raise "Release signing material is missing: set KEYSTORE env or provide signing.properties"
    end
  end

  def self.extract_version_name(gradle_file_path = "app/build.gradle.kts")
    content = File.read(gradle_file_path)
    match = content.match(/versionName\s*=\s*["']([^"']+)["']/)
    raise "versionName not found in #{gradle_file_path}" unless match
    match[1]
  end
end
