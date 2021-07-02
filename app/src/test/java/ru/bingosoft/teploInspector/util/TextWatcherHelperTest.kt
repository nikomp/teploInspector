package ru.bingosoft.teploInspector.util

import android.text.Editable
import android.view.View
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.bingosoft.teploInspector.models.Models

@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class TextWatcherHelperTest {

    @Test
    fun testGetView() {
        val fakeControl= Models.TemplateControl(type = "nothing")
        val mockView=mock(View::class.java)
        val textWatcherHelper=TextWatcherHelper(fakeControl,mockView)
        assertEquals(mockView,textWatcherHelper.view)
    }

    @Test
    fun testAfterTextChanged()  {
        val fakeControl= Models.TemplateControl(type = "nothing")
        val mockView=mock(View::class.java)
        val textWatcherHelper=TextWatcherHelper(fakeControl,mockView)

        val fakeEditable=mock(Editable::class.java)
        `when`(fakeEditable.toString()).thenReturn("")
        textWatcherHelper.afterTextChanged(fakeEditable)
        assertFalse(fakeControl.answered)

        `when`(fakeEditable.toString()).thenReturn("fakeString")
        textWatcherHelper.afterTextChanged(fakeEditable)
        assertTrue(fakeControl.answered)
        assertNotNull(fakeControl.resvalue)
    }

    @Test
    fun testAfterTextChangedNumeric()  {
        val mockControl= Models.TemplateControl(type = "numeric")
        val mockView=mock(View::class.java)
        val textWatcherHelper=TextWatcherHelper(mockControl,mockView)


        val fakeEditable=mock(Editable::class.java)
        //`when`(fakeEditable.toString()).thenReturn(null)
        textWatcherHelper.afterTextChanged(null)
        assertEquals("null",mockControl.resvalue)

        `when`(fakeEditable.toString()).thenReturn("")
        textWatcherHelper.afterTextChanged(fakeEditable)
        assertNull(mockControl.resvalue)

        `when`(fakeEditable.toString()).thenReturn("1")
        textWatcherHelper.afterTextChanged(fakeEditable)
        assertNotNull(mockControl.resvalue)
    }

    @Test
    fun testAfterTextChangedCombobox()  {
        val mockControl= Models.TemplateControl(type = "combobox")
        val mockView=mock(View::class.java)
        val textWatcherHelper=TextWatcherHelper(mockControl,mockView)

        val fakeEditable=mock(Editable::class.java)
        textWatcherHelper.afterTextChanged(null)
        assertEquals("null",mockControl.resvalue)

        `when`(fakeEditable.toString()).thenReturn("")
        textWatcherHelper.afterTextChanged(fakeEditable)
        assertNotNull(mockControl.resvalue)

        `when`(fakeEditable.toString()).thenReturn("fakeStringForCombobox")
        textWatcherHelper.afterTextChanged(fakeEditable)
        assertNotNull(mockControl.resvalue)

        `when`(fakeEditable.toString()).thenReturn("fakeString\nForCombobox")
        textWatcherHelper.afterTextChanged(fakeEditable)
        assertNotNull(mockControl.resvalue)
        assertFalse(mockControl.resvalue!!.contains("\n"))
    }

    @Test
    // Тест нужен для повышения покрытия в Jacoco, проверяется нет ли исключений для этого метода.
    // Не особо полезен, но увеличивает охват кода тестами
    // Подробнее тут https://softwareengineering.stackexchange.com/questions/211105/best-practice-for-code-coverage-of-empty-interface-methods
    fun testBeforeTextChanged()  {
        val fakeControl= Models.TemplateControl(type = "nothing")
        val mockView=mock(View::class.java)
        val textWatcherHelper=TextWatcherHelper(fakeControl,mockView)

        textWatcherHelper.beforeTextChanged("x",0,1,1)
    }

    @Test
    // Тест нужен для повышения покрытия в Jacoco, проверяется нет ли исключений для этого метода.
    // Не особо полезен, но увеличивает охват кода тестами
    // Подробнее тут https://softwareengineering.stackexchange.com/questions/211105/best-practice-for-code-coverage-of-empty-interface-methods
    fun testOnTextChanged()  {
        val fakeControl= Models.TemplateControl(type = "nothing")
        val mockView=mock(View::class.java)
        val textWatcherHelper=TextWatcherHelper(fakeControl,mockView)

        textWatcherHelper.onTextChanged("x",0,1,1)
    }
}