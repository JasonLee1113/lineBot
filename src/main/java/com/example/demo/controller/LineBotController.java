package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.MessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/lineBot")
public class LineBotController {

	@Value("${line.bot.channel-token}")
	private String lineBotToken;

	private final RestTemplate restTemplate = new RestTemplate();

	private static final Logger log = LoggerFactory.getLogger(LineBotController.class);

	@PostMapping("/sendMessage")
	public ResponseEntity<String> sendMessage(@RequestBody MessageRequest messageRequest)
			throws JsonProcessingException {
		String lineBotUrl = "https://api.line.me/v2/bot/message/push";
		// 設置Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(lineBotToken);
		log.info("lineid : " + messageRequest.getTo());
		// 建立請求的JSON payload
		String payload = String.format("{\"to\":\"%s\",\"messages\":[{\"type\":\"text\",\"text\":\"%s\"}]}",
				messageRequest.getTo(), messageRequest.getMessage());
		log.info("payload:" + payload);
		HttpEntity<String> entity = new HttpEntity<>(payload, headers);
		ResponseEntity<String> response = restTemplate.exchange(lineBotUrl, HttpMethod.POST, entity, String.class);
		return response;
	}

	/**
	 * Webhook
	 */
	@PostMapping("/webhook")
	private void getUserID(@RequestBody Map<String, Object> request) {
		try {
			log.info("webhook test");
			List<Map<String, Object>> eventList = (List<Map<String, Object>>) request.get("events");
			if (eventList != null) {
				for (var event : eventList) {
					// 取得事件類型:加入好友 or 回覆訊息
					String type = (String) event.get("type");
					Map<String, Object> source = (Map<String, Object>) event.get("source");
					String userID = (String) source.get("userId");

					if ("message".equals(type)) {
						// reply message to lineBot
						String userName = getUserInfor(userID);

						log.info("有好友回覆機器人訊息:" + userName);
					} else if ("follow".equals(type)) {
						// 加入機器人為好友
						String userName = getUserInfor(userID);
						log.info("用戶加入機器人為好友:" + userName);
					}
				}
			}
		} catch (Exception e) {
			log.error("例外訊息:" + e.toString());
		}

	}

	// 取得收訊息者資訊
	private String getUserInfor(String userID) {

		String userName = "";
		try {
			String url = "https://api.line.me/v2/bot/profile/" + userID;

			HttpHeaders header = new HttpHeaders();
			header.setBearerAuth(lineBotToken);

			HttpEntity<String> entity = new HttpEntity<>(header);

			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			String responseBody = response.getBody();

			JSONObject responseToJson = new JSONObject(responseBody);

			userName = responseToJson.optString("displayName");
			log.info("userName = " + userName);

		} catch (Exception e) {
			log.error("取得收訊息者資訊:" + e.toString());
		}
		return userName;
	}

}
