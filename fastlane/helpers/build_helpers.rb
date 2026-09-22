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
end
