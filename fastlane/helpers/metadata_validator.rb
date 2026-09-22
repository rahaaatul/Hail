class MetadataValidator
  LOCALES = %w[en-US zh-CN].freeze
  METADATA_ROOT = File.expand_path("../../fastlane/metadata/android", __dir__).freeze

  def validate!(metadata_root: METADATA_ROOT)
    LOCALES.each do |locale|
      locale_dir = File.join(metadata_root, locale)
      raise "#{locale}: missing title.txt" unless File.file?(File.join(locale_dir, "title.txt"))
      raise "#{locale}: missing short_description.txt" unless File.file?(File.join(locale_dir, "short_description.txt"))
      raise "#{locale}: missing full_description.txt" unless File.file?(File.join(locale_dir, "full_description.txt"))
      raise "#{locale}: missing icon.png" unless File.file?(File.join(locale_dir, "images", "icon.png"))
      screenshots = Dir.glob(File.join(locale_dir, "images", "phoneScreenshots", "*.png"))
      raise "#{locale}: expected at least 2 screenshots, found #{screenshots.size}" if screenshots.size < 2
    end
    true
  end
end
