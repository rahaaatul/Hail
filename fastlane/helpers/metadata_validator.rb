class MetadataValidator
  METADATA_ROOT = File.expand_path("../../fastlane/metadata/android", __dir__).freeze
  REQUIRED_TEXT_FILES = %w[title.txt short_description.txt full_description.txt].freeze

  def validate!(metadata_root: METADATA_ROOT)
    unless File.directory?(metadata_root)
      if File.exist?(metadata_root)
        raise "metadata root is not a directory: #{metadata_root}"
      end

      raise "metadata root does not exist: #{metadata_root}"
    end

    locales = Dir.children(metadata_root)
              .select { |entry| File.directory?(File.join(metadata_root, entry)) }
              .sort
    raise "metadata root contains no locale directories: #{metadata_root}" if locales.empty?

    locales.each do |locale|
      locale_dir = File.join(metadata_root, locale)
      REQUIRED_TEXT_FILES.each do |filename|
        path = File.join(locale_dir, filename)
        raise "#{locale}: missing #{filename}" unless File.file?(path)
        raise "#{locale}: #{filename} is empty" if File.read(path).strip.empty?
      end
      raise "#{locale}: missing icon.png" unless File.file?(File.join(locale_dir, "images", "icon.png"))
      screenshots = Dir.glob(File.join(locale_dir, "images", "phoneScreenshots", "*.png")).sort
      raise "#{locale}: expected at least 2 screenshots, found #{screenshots.size}" if screenshots.size < 2
    end
    true
  end
end
