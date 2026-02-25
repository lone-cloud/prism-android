SHELL := /bin/bash

GRADLEW := ./gradlew
WORKFLOW := release.yml

.PHONY: debug release-local lint release check-updates

check-updates:
	$(GRADLEW) :app:dependencyUpdates --no-configuration-cache
	@echo "Direct dependency updates:" && \
	grep -oP 'module\s*=\s*"\K[^"]+' gradle/libs.versions.toml | \
	sed 's|.*|^ - & \\[|' | \
	grep -hEf - app/build/dependencyUpdates/report.txt \
	|| echo "All direct dependencies are up to date."

debug:
	$(GRADLEW) assembleDebug --stacktrace

release-local:
	@set -a && . ./.env && set +a && \
	KEYSTORE_TMP=$$(mktemp /tmp/prism-release-XXXXXX.keystore) && \
	echo "$$ANDROID_SIGNING_KEYSTORE_B64" | tr -d '[:space:]' | base64 -d > "$$KEYSTORE_TMP" && \
	ANDROID_SIGNING_STORE_FILE="$$KEYSTORE_TMP" \
	$(GRADLEW) assembleRelease --stacktrace; \
	EXIT=$$?; rm -f "$$KEYSTORE_TMP"; exit $$EXIT

lint:
	$(GRADLEW) ktlintCheck detekt --stacktrace

release:
	@gh auth status >/dev/null
	$(eval TAG := v$(shell cat VERSION))
	@echo "Releasing $(TAG)"
	git tag $(TAG)
	git push origin $(TAG)
	gh workflow run $(WORKFLOW) --ref $(TAG) -f tag=$(TAG)
	gh run list --workflow $(WORKFLOW) --limit 1 | cat
