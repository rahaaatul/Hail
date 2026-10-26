import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.fragment.app.Fragment
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentExtensionsTest {
    @Test
    fun `isLandscape delegates to requireContext`() {
        val fragment = mockk<Fragment>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)
        val config =
            Configuration().apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
            }

        every { resources.configuration } returns config
        every { context.resources } returns resources
        every { fragment.requireContext() } returns context

        assertTrue(fragment.isLandscape)
    }

    @Test
    fun `isRtl delegates to requireContext`() {
        val fragment = mockk<Fragment>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)
        val config = mockk<Configuration>(relaxed = true)

        every { config.layoutDirection } returns View.LAYOUT_DIRECTION_RTL
        every { resources.configuration } returns config
        every { context.resources } returns resources
        every { fragment.requireContext() } returns context

        assertTrue(fragment.isRtl)
    }
}
