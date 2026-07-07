# ARCHITECTURE
## Overview
The smsforward project is a single-module application with a simple architecture. The following sections provide a detailed overview of the project's architecture, components, and technology choices.

## High-Level Architecture Diagram
```
                      +---------------+
                      |  App (src)  |
                      +---------------+
                             |
                             |
                             v
                      +---------------+
                      | androidTest  |
                      |  test         |
                      +---------------+
                             |
                             |
                             v
                      +---------------+
                      |  totp_authenticator  |
                      |  i2c_scanner         |
                      +---------------+
                             |
                             |
                             v
                      +---------------+
                      |  Gradle (Build)  |
                      +---------------+
```

## Component Descriptions
### App (src)
The App component contains the source code for the application, including the main application logic, Android tests, and unit tests.

### androidTest and test
These components contain the test cases for the application, including Android tests and unit tests.

### totp_authenticator and i2c_scanner
These components are part of the totp_authenticator module and contain the implementation for time-based one-time password authentication and I2C scanning, respectively.

### Gradle (Build)
The Gradle component is responsible for building and managing the project's dependencies.

## Data Flow
The data flow in the application is straightforward:
1. The App component receives input from the user and processes it.
2. The processed data is then passed to the totp_authenticator component for authentication.
3. The authenticated data is then scanned using the i2c_scanner component.
4. The results are then returned to the App component for display.

## Key Patterns Used
* **Single-Module Architecture**: The application is structured as a single module, making it easy to manage and maintain.
* **Separation of Concerns**: The application separates the concerns of authentication, scanning, and testing into separate components.

## Technology Choices and Rationale
* **Gradle**: Gradle is used as the build system and package manager due to its flexibility, scalability, and ease of use.
* **Python, C++, C**: These languages are used for implementation due to their performance, reliability, and versatility.
* **No Frameworks or Libraries**: No external frameworks or libraries are used, keeping the application lightweight and easy to maintain.
* **No Database**: No database is used, as the application does not require persistent data storage.
* **No CI/CD**: No Continuous Integration/Continuous Deployment (CI/CD) pipeline is used, as the application is still in development.