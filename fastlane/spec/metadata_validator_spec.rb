require "fileutils"
require "minitest/autorun"
require "tmpdir"
require_relative "../helpers/metadata_validator"

class MetadataValidatorSpec < Minitest::Test
  def setup
    @metadata_root = Dir.mktmpdir("metadata-validator")
  end

  def teardown
    FileUtils.remove_entry(@metadata_root) if File.directory?(@metadata_root)
  end

  def test_validates_discovered_locale_directories
    create_locale("fr-FR")

    assert MetadataValidator.new.validate!(metadata_root: @metadata_root)
  end

  def test_discovers_locale_directories_in_deterministic_order
    create_locale("zh-CN")
    create_locale("en-US")
    FileUtils.rm_f(File.join(@metadata_root, "en-US", "title.txt"))
    FileUtils.rm_f(File.join(@metadata_root, "zh-CN", "title.txt"))

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_match(/\Aen-US:/, error.message)
  end

  def test_missing_title_raises_clear_error
    create_locale("en-US")
    FileUtils.rm_f(File.join(@metadata_root, "en-US", "title.txt"))

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_includes(error.message, "en-US")
    assert_includes(error.message, "title.txt")
  end

  def test_empty_title_raises_clear_error
    create_locale("en-US")
    FileUtils.mkdir_p(File.join(@metadata_root, "en-US"))
    File.write(File.join(@metadata_root, "en-US", "title.txt"), "   \n")

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_includes(error.message, "en-US")
    assert_includes(error.message, "title.txt")
    assert_includes(error.message, "empty")
  end

  def test_missing_short_description_raises_clear_error
    create_locale("en-US")
    FileUtils.rm_f(File.join(@metadata_root, "en-US", "short_description.txt"))

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_includes(error.message, "en-US")
    assert_includes(error.message, "short_description.txt")
  end

  def test_missing_full_description_raises_clear_error
    create_locale("en-US")
    FileUtils.rm_f(File.join(@metadata_root, "en-US", "full_description.txt"))

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_includes(error.message, "en-US")
    assert_includes(error.message, "full_description.txt")
  end

  def test_missing_icon_raises_clear_error
    create_locale("en-US")
    FileUtils.rm_f(File.join(@metadata_root, "en-US", "images", "icon.png"))

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_includes(error.message, "en-US")
    assert_includes(error.message, "icon.png")
  end

  def test_requires_at_least_two_phone_screenshots
    create_locale("en-US")
    FileUtils.rm_f(File.join(@metadata_root, "en-US", "images", "phoneScreenshots", "2.png"))

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_includes(error.message, "en-US")
    assert_includes(error.message, "at least 2 screenshots")
  end

  def test_empty_metadata_root_raises_clear_error
    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: @metadata_root)
    end

    assert_includes(error.message, "no locale directories")
    assert_includes(error.message, @metadata_root)
  end

  def test_missing_metadata_root_raises_clear_error
    metadata_root = File.join(@metadata_root, "missing")

    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: metadata_root)
    end

    assert_includes(error.message, "does not exist")
    assert_includes(error.message, metadata_root)
  end

  private

  def create_locale(locale)
    locale_dir = File.join(@metadata_root, locale)
    images_dir = File.join(locale_dir, "images")
    screenshots_dir = File.join(images_dir, "phoneScreenshots")
    FileUtils.mkdir_p(screenshots_dir)
    %w[title.txt short_description.txt full_description.txt].each do |filename|
      File.write(File.join(locale_dir, filename), "metadata")
    end
    File.write(File.join(images_dir, "icon.png"), "icon")
    File.write(File.join(screenshots_dir, "1.png"), "screenshot")
    File.write(File.join(screenshots_dir, "2.png"), "screenshot")
  end
end
