package com.promiseledger.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.demo-user")
public record DemoUserProperties(boolean enabled, String username, String password) {

	@Override
	public String toString() {
		return "DemoUserProperties[enabled=" + this.enabled + ", username=" + this.username + ", password=****]";
	}

}
