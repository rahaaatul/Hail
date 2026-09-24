require "minitest/autorun"
require_relative "../helpers/telegram_helper"

class TelegramHelperSpec < Minitest::Test
  def test_missing_token_returns_dry_run_without_running_curl
    capture_called = false
    with_capture3 do |*_args|
      capture_called = true
      ["", "", successful_status]
      TelegramHelper.notify(
        artifact: "app.apk",
        build_type: "debug",
        token: "",
        group: "123"
      )
    end

    refute capture_called
  end

  def test_notify_returns_sent_result_with_bounded_curl_command
    command = nil
    Open3.stub(:capture3, lambda { |*args|
      command = args
      ["{\"ok\":true}\n200", "", successful_status]
    }) do
      result = TelegramHelper.notify(
        artifact: "app.apk",
        build_type: "release",
        caption: "A & <b>'quoted\"</b>",
        token: "token",
        group: "123"
      )

      assert_equal({ status: :sent, http_code: "200" }, result)
    end

    assert_includes command, "--connect-timeout"
    assert_equal "10", command[command.index("--connect-timeout") + 1]
    assert_includes command, "--max-time"
    assert_equal "30", command[command.index("--max-time") + 1]
    assert_includes command, "caption=A &amp; &lt;b&gt;&#39;quoted&quot;&lt;/b&gt;"
    assert_includes command, "message_thread_id=85"
  end

  def test_notify_raises_on_non_200_response
    Open3.stub(:capture3, lambda { |*_args|
      ["{\"ok\":false}\n400", "bad gateway", successful_status]
    }) do
      error = assert_raises(RuntimeError) do
        TelegramHelper.notify(
          artifact: "app.apk",
          build_type: "debug",
          token: "token",
          group: "123"
        )
      end

      assert_includes error.message, "HTTP 400"
      assert_includes error.message, "bad gateway"
    end
  end

  def test_notify_raises_when_curl_fails
    Open3.stub(:capture3, lambda { |*_args|
      ["\n000", "curl: (28) Connection timed out", failed_status]
    }) do
      error = assert_raises(RuntimeError) do
        TelegramHelper.notify(
          artifact: "app.apk",
          build_type: "debug",
          token: "token",
          group: "123"
        )
      end

      assert_includes error.message, "Connection timed out"
    end
  end

  def test_unknown_build_type_raises
    error = assert_raises(RuntimeError) do
      TelegramHelper.notify(
        artifact: "app.apk",
        build_type: "staging",
        token: "token",
        group: "123"
      )
    end

    assert_includes error.message, "Unknown build_type"
    assert_includes error.message, "staging"
  end

  def test_pr_number_takes_precedence_over_build_type_topic
    assert_equal 218, TelegramHelper.topic_for("debug", 42)
  end

  private

  def successful_status
    status = Object.new
    status.define_singleton_method(:success?) { true }
    status
  end

  def failed_status
    status = Object.new
    status.define_singleton_method(:success?) { false }
    status.define_singleton_method(:exitstatus) { 28 }
    status
  end
end
