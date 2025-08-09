# Gemini - Unofficial Android Client

This is a fully-featured, unofficial Android client for Google's Gemini, built in Java. It is based on a reverse-engineered API and provides a clean, conversational interface for interacting with the Gemini models.

This project was created by translating the unofficial Python `gemini-webapi` library into a native Android application.

## Features

- **Full Chat Interface:** A clean, modern chat UI built with Material 3 components.
- **Multi-Turn Conversations:** Supports stateful, multi-turn conversations. Your chat history is maintained and sent with each request.
- **Conversation Management:** All conversations are automatically saved. You can switch between past conversations or start a new one from the navigation drawer.
- **File Uploads:** Supports uploading files alongside your text prompts.
- **Image Support:** Displays web images returned by the Gemini API.
- **Persistent Storage:** Chat history and API credentials are saved locally on your device using `SharedPreferences`.
- **Automated Builds:** Includes a GitHub Actions workflow to automatically build and sign a release APK on every push.

## Setup: Getting Your API Credentials

This app uses a reverse-engineered API that relies on browser cookies for authentication. To use the app, you need to get two cookie values from your browser after logging into the official Gemini website.

1.  Go to `https://gemini.google.com` in a web browser (like Chrome or Firefox) and log in with your Google account.
2.  Open the developer tools. You can usually do this by pressing `F12` or right-clicking the page and selecting "Inspect".
3.  Go to the "Application" (in Chrome) or "Storage" (in Firefox) tab.
4.  Find the "Cookies" section for `https://gemini.google.com`.
5.  Find the following two cookies and copy their values:
    -   `__Secure-1PSID`
    -   `__Secure-1PSIDTS`
6.  Open the Gemini Android app, open the navigation drawer (swipe from the left or tap the menu icon), and go to "Settings".
7.  Paste the cookie values into the corresponding fields and tap "Save". The client will be initialized, and you can start chatting.

## Building from Source

The project is a standard Android Gradle project. You can build it from the command line using the included Gradle wrapper.

1.  Clone the repository:
    ```sh
    git clone https://github.com/your-repo/gemini-android.git
    cd gemini-android
    ```
2.  Make the Gradle wrapper executable (on Linux/macOS):
    ```sh
    chmod +x ./gradlew
    ```
3.  Build the release APK:
    ```sh
    ./gradlew assembleRelease
    ```
4.  The signed APK will be located in `app/build/outputs/apk/release/`.

The app is signed with a hardcoded debug key included in the repository, so no special setup is required.

## Code Structure

The Java source code is organized into several packages based on functionality.

### `com.gemini.api` (Root UI)

-   **`MainActivity.java`**: The single activity in the app. It is responsible for managing the UI, handling user input, setting up the `RecyclerView`, and coordinating between the other components.

### `com.gemini.api.client` (Core API Logic)

-   **`GeminiClient.java`**: The core of the API wrapper. This class handles all networking with the Gemini API. It builds requests, sends them via OkHttp, and parses the complex responses.
-   **`ChatSession.java`**: A stateful class that manages a single conversation thread. It holds the conversation metadata (`cid`, `rid`, `rcid`) required to maintain context in multi-turn chats.
-   **`models/`**: This subpackage contains all the POJO (Plain Old Java Object) classes that represent the data structures returned by the API, such as `ModelOutput`, `Candidate`, and `Image`.

### `com.gemini.api.persistence` (Data Storage)

-   **`Conversation.java`**: A data class representing a single conversation, containing an ID, title, and a list of chat messages.
-   **`ConversationManager.java`**: A helper class that handles saving and loading `Conversation` objects to and from `SharedPreferences`. It uses the Gson library to serialize conversations into JSON strings for storage.

### `com.gemini.api.ui` (UI Components)

-   **`ChatMessage.java`**: A simple data class representing a single message to be displayed in the UI.
-   **`ChatAdapter.java`**: The `RecyclerView.Adapter` responsible for managing the list of `ChatMessage` objects and binding them to the correct chat bubble layouts (`item_chat_user.xml` or `item_chat_gemini.xml`).
