package io.zenandroid.onlinego.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.HorizontalPagerIndicator
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme

@ExperimentalFoundationApi
@Composable
fun Screen(state: OnboardingState, listener: (OnboardingAction) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(36.dp, 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PageIndicator(
                currentPage = state.currentPageIndex,
                numberOfPages = state.totalPages,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            when (state.currentPage) {
                is Page.OnboardingPage -> InfoPage(
                    page = state.currentPage,
                    listener = listener
                )
                is Page.MultipleChoicePage -> QuestionPage(
                    page = state.currentPage,
                    listener = listener
                )
                is Page.LoginPage -> when (state.loginMethod!!) {
                    Page.LoginMethod.GOOGLE -> {
                    }
//                    Page.LoginMethod.FACEBOOK -> {}
                    Page.LoginMethod.PASSWORD -> LoginPage(
                        state = state,
                        listener = listener
                    )
                }

            }
        }
    }
}

@Composable
private fun ColumnScope.LoginPage(
    state: OnboardingState,
    listener: (OnboardingAction) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.weight(.5f))
        Image(
            painter = painterResource(id = R.drawable.art_login_1),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.weight(.5f))

        if (!state.isExistingAccount) {
            OutlinedTextField(
                value = state.email,
                label = { Text("Email") },
                enabled = !state.loginProcessing,
                onValueChange = { listener.invoke(OnboardingAction.EmailChanged(it)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = state.username,
            label = { Text("Username") },
            enabled = !state.loginProcessing,
            onValueChange = { listener.invoke(OnboardingAction.UsernameChanged(it)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
        )

        var passwordVisibility by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = state.password,
            label = { Text("Password") },
            enabled = !state.loginProcessing,
            onValueChange = { listener.invoke(OnboardingAction.PasswordChanged(it)) },
            visualTransformation = if(!passwordVisibility) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                    Icon(
                        imageVector  = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = ""
                    )
                }
            },
            keyboardActions = KeyboardActions(
                onDone = { if (state.logInButtonEnabled) listener.invoke(OnboardingAction.LoginPressed) }
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.weight(.5f))
    }

    Button(
        onClick = { listener.invoke(OnboardingAction.LoginPressed) },
        enabled = state.logInButtonEnabled && !state.loginProcessing,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        if(!state.loginProcessing) {
            Text(
                text = if (state.isExistingAccount) "Link OGS account" else "Create OGS account",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            CircularProgressIndicator()
        }
    }

    if(state.loginErrorDialogText != null) {
        AlertDialog(
            onDismissRequest = { listener.invoke(OnboardingAction.DialogDismissed) },
            confirmButton = {
                Button(onClick = { listener.invoke(OnboardingAction.DialogDismissed) }) {
                    Text(text = "OK")
                }
            },
            title = { Text(text = "Log in failed") },
            text = { Text(text = state.loginErrorDialogText) }
        )
    }
}

@Composable
private fun ColumnScope.QuestionPage(page: Page.MultipleChoicePage, listener: (OnboardingAction) -> Unit) {
    Spacer(modifier = Modifier.weight(.5f))
    Text(
        text = page.question,
        style = MaterialTheme.typography.h6,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(modifier = Modifier.weight(.5f))
    for (answer in page.answers) {
        Button(
            onClick = { listener.invoke(OnboardingAction.AnswerSelected(page.answers.indexOf(answer))) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = answer, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(modifier = Modifier.weight(2f))
}

@Composable
private fun ColumnScope.InfoPage(page: Page.OnboardingPage, listener: (OnboardingAction) -> Unit) {
    Image(
        painter = painterResource(id = page.art),
        contentDescription = null,
        modifier = Modifier
            .padding(vertical = 70.dp)
            .weight(.5f)
            .align(Alignment.CenterHorizontally)
            .fillMaxWidth()
    )
    Text(
        text = page.title,
        style = MaterialTheme.typography.h6,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Text(
        text = page.description,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.body1,
        lineHeight = 20.sp,
        modifier = Modifier
            .weight(.5f)
            .wrapContentHeight(Alignment.CenterVertically)
            .align(Alignment.CenterHorizontally)
    )
    Button(onClick = { listener(OnboardingAction.ContinueClicked) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = page.continueButtonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@ExperimentalFoundationApi
@Composable
private fun PageIndicator(modifier: Modifier = Modifier, currentPage: Int, numberOfPages: Int) {
    val state = rememberPagerState { numberOfPages }
    HorizontalPager(state = state) {} // Dirty hack to set the number of pages since that's no longer supported

    LaunchedEffect(currentPage) {
        state.animateScrollToPage(currentPage)
    }
    HorizontalPagerIndicator(
        pagerState = state,
        pageCount = state.pageCount,
        activeColor = MaterialTheme.colors.onSurface,
        modifier = modifier
            .padding(16.dp)
            .clickable(enabled = false) {}
    )
}

@ExperimentalFoundationApi
@Preview
@Composable
fun DefaultPreview() {
    OnlineGoTheme (darkTheme = true) {
        Screen(OnboardingState()) { }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun DefaultPreview1() {
    OnlineGoTheme (darkTheme = true) {
        Screen(OnboardingState(currentPage = Page.MultipleChoicePage("Is there a cow level? Lorem ipsum dolor sit amet", listOf("Yes", "NO!!!")))) { }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun DefaultPreview2() {
    OnlineGoTheme (darkTheme = true) {
        Screen(OnboardingState(currentPage = Page.LoginPage, loginMethod = Page.LoginMethod.PASSWORD, isExistingAccount = false)) { }
    }
}
