package com.promiseledger.security;

import com.promiseledger.user.User;
import com.promiseledger.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DemoUserInitializer implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(DemoUserInitializer.class);

	private final DemoUserProperties properties;

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public DemoUserInitializer(DemoUserProperties properties, UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!this.properties.enabled()) {
			return;
		}
		if (!StringUtils.hasText(this.properties.username()) || !StringUtils.hasText(this.properties.password())) {
			logger.warn("Demo user provisioning is enabled but username or password is blank; skipping.");
			return;
		}
		String username = this.properties.username().trim();
		if (this.userRepository.existsByUsernameIgnoreCase(username)) {
			return;
		}
		this.userRepository.save(new User(username, this.passwordEncoder.encode(this.properties.password())));
		logger.info("Provisioned demo user '{}'", username);
	}

}
