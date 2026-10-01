package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("دارینو", appName)
  }

  @Test
  fun `vehicle mock data loads correctly with multiple vehicles`() {
    val vehicles = com.example.ui.screens.vehicle.model.VehicleMockDataSource.sampleVehicles
    assertEquals(2, vehicles.size)

    val peugeot = vehicles[0]
    assertEquals("پژو ۲۰۶ تیپ ۵", peugeot.name)
    assertEquals(125800L, peugeot.odometerKm)
    assertEquals(3, peugeot.upcomingServices.size)
    assertEquals(3, peugeot.recentExpenses.size)

    val dena = vehicles[1]
    assertEquals("دنا پلاس توربو", dena.name)
    assertEquals(82400L, dena.odometerKm)
  }

  @Test
  fun `persian digit formatting converts correctly`() {
    val persianDigits = com.example.ui.designsystem.AppFormatters.toPersianDigits("1234567890")
    assertEquals("۱۲۳۴۵۶۷۸۹۰", persianDigits)

    val amountFormatted = com.example.ui.designsystem.AppFormatters.formatAmount(18500000L)
    assertEquals("۱۸,۵۰۰,۰۰۰", amountFormatted)

    val currencyFormatted = com.example.ui.designsystem.AppFormatters.formatCurrency(4200000L)
    assertEquals("۴,۲۰۰,۰۰۰ تومان", currencyFormatted)

    val kmFormatted = com.example.ui.designsystem.AppFormatters.formatKm(125800L)
    assertEquals("۱۲۵,۸۰۰ کیلومتر", kmFormatted)
  }

  @Test
  fun `bottom navigation has items including installments and vehicle`() {
    val items = com.example.ui.screens.home.components.BottomNavItem.entries
    assertEquals(7, items.size)
    assertEquals(com.example.ui.screens.home.components.BottomNavItem.HOME, items[0])
    assertEquals(com.example.ui.screens.home.components.BottomNavItem.FINANCE, items[1])
    assertEquals(com.example.ui.screens.home.components.BottomNavItem.CALENDAR, items[2])
    assertEquals(com.example.ui.screens.home.components.BottomNavItem.INSTALLMENTS, items[3])
    assertEquals(com.example.ui.screens.home.components.BottomNavItem.VEHICLE, items[4])
    assertEquals(com.example.ui.screens.home.components.BottomNavItem.REMINDERS, items[5])
    assertEquals(com.example.ui.screens.home.components.BottomNavItem.REPORTS, items[6])
    assertEquals("اقساط", items[3].title)
    assertEquals("خودرو", items[4].title)
  }

  @Test
  fun `installments mock data loads categories and summary correctly`() {
    val summary = com.example.ui.screens.installments.model.InstallmentMockDataSource.summary
    assertEquals(6, summary.activeCount)
    val categories = com.example.ui.screens.installments.model.InstallmentMockDataSource.categorySummaries
    assertEquals(4, categories.size)
    assertEquals("وام‌های بانکی", categories[0].category.title)
    assertEquals("وام‌های خانگی", categories[1].category.title)
    assertEquals("اقساط بیمه خودرو", categories[2].category.title)
    assertEquals("اقساط متفرقه", categories[3].category.title)
  }
}
