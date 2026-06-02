SHELL        = cmd.exe
.SHELLFLAGS  = /c

JAVA_HOME    := C:\Program Files\Android\Android Studio\jbr
ANDROID_HOME := $(USERPROFILE)\AppData\Local\Android\Sdk
ADB          := $(ANDROID_HOME)\platform-tools\adb.exe
EMULATOR     := $(ANDROID_HOME)\emulator\emulator.exe
AVD          := Pixel_8_API_35
APP_ID       := com.hz.appon
MAIN         := com.hz.appon/.MainActivity
APK          := app\build\outputs\apk\debug\app-debug.apk

.PHONY: help build test clean install run emulator kill logs

help:
	@echo.
	@echo   make build     - compile debug APK
	@echo   make test      - run unit tests
	@echo   make clean     - delete build outputs
	@echo   make emulator  - start the Android emulator
	@echo   make install   - install debug APK on running emulator/device
	@echo   make run       - build + install + launch app
	@echo   make kill      - force-stop the app on device
	@echo   make logs      - stream logcat filtered to this app
	@echo.

build:
	set "JAVA_HOME=$(JAVA_HOME)" && .\gradlew.bat assembleDebug

test:
	set "JAVA_HOME=$(JAVA_HOME)" && .\gradlew.bat test

clean:
	set "JAVA_HOME=$(JAVA_HOME)" && .\gradlew.bat clean

emulator:
	start "" "$(EMULATOR)" -avd $(AVD) -no-snapshot-load -gpu host

install: build
	"$(ADB)" install -r $(APK)

run: install
	"$(ADB)" shell am start -n $(MAIN)

kill:
	"$(ADB)" shell am force-stop $(APP_ID)

logs:
	"$(ADB)" logcat --pid=$$($(ADB) shell pidof -s $(APP_ID))
