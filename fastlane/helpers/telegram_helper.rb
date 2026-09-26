require "open3"

class TelegramHelper
  TOPIC_MAP = {
    "debug" => 84,
    "release" => 85,
    "pre-release" => 95,
    "pr" => 218
  }.freeze

  def self.notify(artifact:, build_type:, pr_number: nil, caption: "", token:, group:, topic: nil)
    return { status: :dry_run } if token.to_s.empty?

    topic ||= topic_for(build_type, pr_number)
    url = "https://api.telegram.org/bot#{token}/sendDocument"
    caption = escape_html(caption)
    args = [
      "curl", "-sS", "--connect-timeout", "10", "--max-time", "30", "-w", "\n%{http_code}",
      "-F", "chat_id=#{group}",
      "--form-string", "caption=#{caption}",
      "--form-string", "parse_mode=HTML",
      "-F", "document=@#{artifact}",
      url
    ]
    args += ["-F", "message_thread_id=#{topic}"] if topic

    stdout, stderr, status = Open3.capture3(*args)
    http_code = stdout.lines.last&.strip
    return { status: :sent, http_code: http_code } if status.success? && http_code == "200"

    raise "Telegram upload failed: HTTP #{http_code} - #{stderr}"
  end

  def self.topic_for(build_type, pr_number)
    return 218 if pr_number.to_s.length.positive?
    TOPIC_MAP.fetch(build_type.to_s) { raise "Unknown build_type: #{build_type}" }
  end

  private

  def self.escape_html(text)
    text.to_s
      .gsub("&", "&")
      .gsub("<", "<")
      .gsub(">", ">")
      .gsub("'", "'")
      .gsub('"', """)
  end
end
