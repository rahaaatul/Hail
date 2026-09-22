require "minitest/autorun"
require_relative "../helpers/build_helpers"

class BuildHelpersSpec < Minitest::Test
  def test_missing_pr_number_raises
    error = assert_raises(RuntimeError) do
      BuildHelpers.validate_pr_number(nil)
    end
    assert_includes(error.message, "PR_NUMBER")
  end
end
