SHELL := /bin/bash

GRADLEW := ./gradlew
WORKFLOW := release.yml

.PHONY: debug release-local lint release check-updates

check-updates:
	@set -e; \
	UPDATE_FILE=gradle/libs.versions.updates.toml; \
	CHECK_LOG=$$(mktemp /tmp/prism-check-updates-XXXXXX.log); \
	rm -f "$$UPDATE_FILE"; \
	$(GRADLEW) versionCatalogUpdate --interactive --no-configuration-cache --no-parallel >"$$CHECK_LOG" 2>&1; \
	echo "Direct dependency updates:"; \
	if [[ -f "$$UPDATE_FILE" ]] && grep -Eq '^[[:space:]]*[A-Za-z0-9_.-]+[[:space:]]*=' "$$UPDATE_FILE"; then \
		grep -E '^[[:space:]]*[A-Za-z0-9_.-]+[[:space:]]*=' "$$UPDATE_FILE"; \
	else \
		echo "All direct dependencies are up to date."; \
	fi; \
	if grep -q "There are libraries that could not be resolved:" "$$CHECK_LOG"; then \
		echo; \
		echo "Unresolved dependencies detected:"; \
		sed -n '/There are libraries that could not be resolved:/,/^$$/p' "$$CHECK_LOG"; \
		rm -f "$$UPDATE_FILE" "$$CHECK_LOG"; \
		exit 1; \
	fi; \
	rm -f "$$UPDATE_FILE" "$$CHECK_LOG"

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
	$(eval VERSION_PARTS := $(subst ., ,$(shell cat VERSION)))
	$(eval MAJOR := $(word 1,$(VERSION_PARTS)))
	$(eval MINOR := $(word 2,$(VERSION_PARTS)))
	$(eval PATCH := $(word 3,$(VERSION_PARTS)))
	$(eval VCODE := $(shell echo $$(($(MAJOR) * 10000 + $(MINOR) * 100 + $(PATCH)))))
	@echo -n "$(VCODE)" > VERSION_CODE
	$(eval CHANGELOG := fastlane/metadata/android/en-US/changelogs/$(VCODE).txt)
	@if [[ ! -f "$(CHANGELOG)" ]]; then \
		$${EDITOR:-nano} "$(CHANGELOG)"; \
	fi
	@if [[ ! -s "$(CHANGELOG)" ]]; then \
		echo "Aborting: changelog is empty."; \
		rm -f "$(CHANGELOG)"; \
		exit 1; \
	fi
	git add "$(CHANGELOG)"
	git diff --cached --quiet || git commit -m "Add changelog for $(TAG)"
	@echo "Releasing $(TAG)"
	git tag $(TAG)
	git push origin $(TAG)
	gh workflow run $(WORKFLOW) --ref $(TAG) -f tag=$(TAG)
	gh run list --workflow $(WORKFLOW) --limit 1 | cat
