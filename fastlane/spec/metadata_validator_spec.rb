require "minitest/autorun"
require_relative "../helpers/metadata_validator"

class MetadataValidatorSpec < Minitest::Test
  def test_missing_title_raises
    error = assert_raises(RuntimeError) do
      MetadataValidator.new.validate!(metadata_root: "fastlane/metadata/android/missing-locale")
    end
    assert_includes(error.message, "title.txt")
  end
end
