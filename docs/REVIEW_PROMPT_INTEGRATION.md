# App Review Integration

This document describes the implementation of Google Play In-App Review integration for the OnlineGo
app.

## Overview

The review prompt system follows Google's guidelines and prompts users to rate the app after they
win a game, with configurable timing and cooldown periods.

## Features

- **Configurable timing**: Users are prompted after using the app for a configurable number of
  days (default: 7 days)
- **Cooldown period**: 2-week cooldown after dismissal to avoid being intrusive
- **Game win detection**: Prompts are triggered only after the user wins a game
- **Google Play In-App Review**: Uses the official Google Play In-App Review API when available
- **Fallback support**: Falls back to Play Store redirect if in-app review fails
- **Analytics tracking**: Comprehensive analytics events for review prompt interactions
- **Google guidelines compliance**: Follows Google's best practices for review prompts

## Implementation Details

### Components

1. **ReviewPromptRepository**: Manages review prompt state using DataStore
2. **ReviewPromptManager**: Handles the review flow and Google Play integration
3. **GameViewModel**: Integrates review prompts with game win detection
4. **GameUI**: UI layer that triggers review prompts

### Configuration

The system is configured with the following constants in `ReviewPromptRepository`:

```kotlin
private const val DAYS_BEFORE_FIRST_PROMPT = 7L // Configurable: 7 days
private const val COOLDOWN_DAYS = 14L // 2 weeks cooldown
```

### Analytics Events

The following analytics events are tracked:

- `review_prompt_shown`: When a review prompt is displayed
- `review_prompt_dismissed`: When user dismisses the prompt
- `review_prompt_rated`: When user rates the app
- `review_prompt_completed_in_app`: When in-app review is completed
- `review_prompt_dismissed_in_app`: When in-app review is dismissed
- `review_prompt_redirected_to_play_store`: When falling back to Play Store
- `review_prompt_redirected_to_browser`: When falling back to browser
- `app_rated_externally`: When app is marked as rated externally
- `review_state_reset`: When review state is reset (testing)

### State Management

The system tracks the following state:

- `app_first_launch_time`: When the app was first launched
- `last_review_prompt_time`: When the last review prompt was shown
- `review_prompt_dismissed`: Whether the user dismissed the prompt
- `review_prompt_rated`: Whether the user has rated the app

### Integration Points

1. **Game Win Detection**: In `GameViewModel.onGameChanged()`, when a game finishes and the player
   wins
2. **UI Trigger**: In `GameUI.kt`, a `LaunchedEffect` monitors `shouldShowReviewPrompt` state
3. **Dependency Injection**: Added to Koin modules for proper dependency management

## Usage

### Automatic Triggering

The review prompt is automatically triggered when:

1. User wins a game
2. App has been used for the configured number of days
3. Not in cooldown period
4. User hasn't already rated the app

### Manual Testing

For testing purposes, you can use the following methods:

```kotlin
// Force show review prompt (bypasses all conditions)
reviewPromptManager.forceShowReviewPrompt(activity)

// Reset review state (for testing)
reviewPromptManager.resetReviewState()

// Mark app as rated externally
reviewPromptManager.markAppAsRated()
```

### Configuration Changes

To modify the timing, update the constants in `ReviewPromptRepository`:

```kotlin
private const val DAYS_BEFORE_FIRST_PROMPT = 7L // Change this value
private const val COOLDOWN_DAYS = 14L // Change this value
```

## Google Guidelines Compliance

The implementation follows Google's guidelines:

1. **Timing**: Prompts are shown at natural break points (after game wins)
2. **Frequency**: Respects cooldown periods to avoid being intrusive
3. **User Experience**: Uses Google Play In-App Review API when available
4. **Fallback**: Gracefully falls back to Play Store redirect
5. **Analytics**: Tracks user interactions for optimization

## Testing

### Test Scenarios

1. **First-time user**: Should not be prompted until after 7 days
2. **Game win**: Should be prompted after winning a game (if conditions are met)
3. **Dismissal**: Should not be prompted again for 2 weeks after dismissal
4. **Rating**: Should not be prompted again after rating
5. **Cooldown**: Should respect 2-week cooldown period

### Debug Information

You can check the current review prompt state:

```kotlin
val state = reviewPromptRepository.getReviewPromptState()
// Returns: ReviewPromptState with all current values
```

## Dependencies

- Google Play Core Library (version 2.0.2)
- Firebase Analytics (for event tracking)
- Firebase Crashlytics (for error reporting)
- DataStore (for state persistence)

## Future Enhancements

Potential improvements:

1. A/B testing for different timing configurations
2. More sophisticated user engagement metrics
3. Integration with user feedback systems
4. Custom review prompt UI for better branding
