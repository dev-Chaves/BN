package com.bnfix.ubm;

import com.bnfix.ubm.shared.nativeimage.NativeRuntimeHints;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(NativeRuntimeHints.class)
public class UbmApplication {

	public static void main(String[] args) {
		SpringApplication.run(UbmApplication.class, args);
	}

}
