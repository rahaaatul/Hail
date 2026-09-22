require "minitest/autorun"
require_relative "../helpers/telegram_helper"

class TelegramHelperSpec < Minitest::Test
  def test_missing_token_returns_dry_run
    result = TelegramHelper.notify(artifact: "app.apk", build_type: "debug", token: nil, group: "123")
    assert_equal :dry_run, result[:status]
  end
end
