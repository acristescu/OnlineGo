package io.zenandroid.onlinego.ui.screens.supporter

import io.zenandroid.onlinego.data.billing.SupporterProduct
import io.zenandroid.onlinego.data.billing.SupporterPurchase
import io.zenandroid.onlinego.data.billing.SupporterStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SupporterViewModelTest {

  private val products = listOf(
    product("supporter_10"),
    product("donation_1"),
    product("supporter_3"),
    product("supporter_5"),
  )

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun keepsSupporterTiersSortedByPriceAndPicksTheMiddleOneForNewSupporters() = runTest {
    val state = loadedState(FakeStore(products, emptyList()))

    assertEquals(
      listOf("supporter_3", "supporter_5", "supporter_10"),
      state.products?.map { it.id })
    assertFalse(state.supporter)
    assertEquals(1, state.selectedTier)
    assertTrue(state.subscribeButtonEnabled)
    assertEquals("€5", state.selectedTierAmount)
  }

  @Test
  fun selectsTheCurrentTierAndEnablesSubscribeOnlyForAnotherOne() = runTest {
    val viewModel = SupporterViewModel(FakeStore(products, listOf(purchase("supporter_10"))))
    val state = viewModel.loaded()

    assertTrue(state.supporter)
    assertEquals(2, state.selectedTier)
    assertEquals("€10", state.currentContributionAmount)
    assertFalse(state.subscribeButtonEnabled)

    viewModel.onUserDragSlider(0.4f)
    assertEquals(0, viewModel.state.value.selectedTier)
    assertTrue(viewModel.state.value.subscribeButtonEnabled)

    viewModel.onUserDragSlider(2f)
    assertFalse(viewModel.state.value.subscribeButtonEnabled)
  }

  @Test
  fun aSubscriptionThatDoesNotRenewIsNotCounted() = runTest {
    val state =
      loadedState(FakeStore(products, listOf(purchase("supporter_5", autoRenewing = false))))

    assertFalse(state.supporter)
    assertEquals(1, state.selectedTier)
  }

  @Test
  fun subscribingLaunchesTheBillingFlowWithTheCurrentPurchase() = runTest {
    val current = purchase("supporter_3")
    val viewModel = SupporterViewModel(FakeStore(products, listOf(current)))
    viewModel.loaded()
    viewModel.onUserDragSlider(2f)
    val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }

    viewModel.onSubscribeClick()

    assertEquals(SupporterEvent.LaunchBillingFlow(product("supporter_10"), current), event.await())
    assertTrue(viewModel.state.value.loading)
  }

  @Test
  fun aFailedQueryReportsTheError() = runTest {
    val viewModel =
      SupporterViewModel(FakeStore(products, emptyList(), error = Exception("offline")))
    val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }

    viewModel.onResume()

    val error = withContext(Dispatchers.Default) { withTimeout(5_000) { event.await() } }
    assertIs<SupporterEvent.ShowError>(error)
    assertEquals("offline", error.throwable.message)
  }

  private suspend fun loadedState(store: SupporterStore) = SupporterViewModel(store).loaded()

  private suspend fun SupporterViewModel.loaded(): SupporterState {
    onResume()
    return withContext(Dispatchers.Default) {
      withTimeout(5_000) { state.first { !it.loading } }
    }
  }

  private fun product(id: String) = SupporterProduct(id, "€${id.substringAfter('_')}")

  private fun purchase(id: String, autoRenewing: Boolean = true) =
    SupporterPurchase(listOf(id), autoRenewing, "token-$id")

  private class FakeStore(
    private val products: List<SupporterProduct>,
    private val purchases: List<SupporterPurchase>,
    private val error: Exception? = null,
  ) : SupporterStore {
    override suspend fun queryPurchases(): List<SupporterPurchase> {
      error?.let { throw it }
      return purchases
    }

    override suspend fun queryAvailableSubscriptions() = products
  }
}
