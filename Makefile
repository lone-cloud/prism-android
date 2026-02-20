SHELL := /bin/bash

GRADLEW := ./gradlew
WORKFLOW := release.yml

.PHONY: debug release-local lint release

debug:
	$(GRADLEW) assembleDebug --stacktrace

release-local:
	$(GRADLEW) assembleRelease --stacktrace

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
