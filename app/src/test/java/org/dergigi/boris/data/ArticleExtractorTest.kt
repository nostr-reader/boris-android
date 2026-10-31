package org.dergigi.boris.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleExtractorTest {
    private val longBody = "The article paragraph carries the real story. ".repeat(15)

    private val chromeWrapped = """
        <html><body>
        <nav>Home</nav>
        <article><p>$longBody</p></article>
        <div id="comments">nope</div>
        </body></html>
    """.trimIndent()

    @Test
    fun newsletterProseSurvivesCleanupWithoutNavigationOrPermalinkIcons() {
        val html = """
            <html><body>
              <header><a href="/">Site logo</a><nav>Mission Funds About</nav></header>
              <main><article>
                <header><h1>Newsletter title</h1></header>
                <div class="newsletter-prose prose prose-lg">
                  <p>$longBody</p>
                  <div class="donate-banner not-prose"><a href="/donate">Donate banner</a></div>
                  <h2 id="building"><a href="#building" aria-hidden="true"><span></span></a>Building the Base</h2>
                  <p>Read <a href="/details">the details</a>. $longBody</p>
                  <img src="/chart.png" alt="Chart">
                </div>
              </article></main>
              <footer>Site footer</footer>
            </body></html>
        """.trimIndent()
        val markdown = ArticleExtractor.markdown(html, "https://example.com/newsletter/quarter")
        assertNotNull(markdown)
        assertTrue(markdown!!.contains(longBody.trim()))
        assertTrue(markdown.contains("## Building the Base"))
        assertTrue(markdown.contains("[the details](https://example.com/details)"))
        assertTrue(markdown.contains("![Chart](https://example.com/chart.png)"))
        assertFalse(markdown.contains("Mission"))
        assertFalse(markdown.contains("Donate banner"))
        assertFalse(markdown.contains("Site footer"))
        assertFalse(markdown.contains("#building"))
    }

    @Test
    fun articleKeepsBodyAndDropsChrome() {
        val node = ArticleExtractor.article(chromeWrapped, "https://example.com/post")
        assertNotNull(node)
        val text = node!!.text()
        assertTrue(text.contains("The article paragraph carries the real story."))
        assertFalse(text.contains("Home"))
        assertFalse(text.contains("nope"))
    }

    @Test
    fun markdownKeepsBodyAndDropsChrome() {
        val markdown = ArticleExtractor.markdown(chromeWrapped, "https://example.com/post")
        assertNotNull(markdown)
        assertTrue(markdown!!.contains("The article paragraph carries the real story."))
        assertFalse(markdown.contains("Home"))
        assertFalse(markdown.contains("nope"))
    }

    @Test
    fun shortTextReturnsNull() {
        val html = "<html><body><article><p>Too short to be an article.</p></article></body></html>"
        assertNull(ArticleExtractor.article(html, "https://example.com/post"))
        assertNull(ArticleExtractor.markdown(html, "https://example.com/post"))
    }

    @Test
    fun linkDenseNodeReturnsNull() {
        val links = (1..30).joinToString(" ") {
            """<a href="/post-$it">Another linked headline number $it in a long index</a>"""
        }
        val html = "<html><body><article>$links</article></body></html>"
        assertNull(ArticleExtractor.article(html, "https://example.com/"))
    }

    @Test
    fun dropsShareChromeAboveTheLede() {
        val html = """
            <html><body>
            <article class="news-article">
              <figure>
                <img src="/hero.jpg" alt="TV">
                <figcaption><span class="credit">(Image credit: Google)</span></figcaption>
              </figure>
              <div id="article-body">
                <div id="utility-bar">
                  <ul>
                    <li><button>Copy link</button></li>
                    <li><a href="https://facebook.com/share">Facebook</a></li>
                    <li><a href="https://x.com/share">X</a></li>
                    <li><a href="https://reddit.com/submit">Reddit</a></li>
                    <li><a href="mailto:x@example.com">Email</a></li>
                  </ul>
                  <a href="/follow">Follow us</a>
                  <span>0</span>
                </div>
                <div class="newsletter-form__wrapper">Newsletter</div>
                <p>I can't tell you the exact number of TVs I've reviewed over the course of my career, but $longBody</p>
              </div>
            </article>
            </body></html>
        """.trimIndent()
        val markdown = ArticleExtractor.markdown(html, "https://example.com/smart")
        assertNotNull(markdown)
        assertTrue(markdown!!.contains("I can't tell you the exact number of TVs"))
        assertFalse(markdown.contains("Copy link"))
        assertFalse(markdown.contains("Facebook"))
        assertFalse(markdown.contains("Follow us"))
        assertFalse(markdown.contains("Newsletter"))
        assertFalse(markdown.contains("Image credit"))
    }

    @Test
    fun keepsFacebookWhenItIsPartOfTheStory() {
        val html = """
            <html><body>
            <article>
              <p>Facebook changed how links work last year, and $longBody</p>
            </article>
            </body></html>
        """.trimIndent()
        val markdown = ArticleExtractor.markdown(html, "https://example.com/post")
        assertNotNull(markdown)
        assertTrue(markdown!!.contains("Facebook changed how links work"))
    }

    @Test
    fun relativeImageResolvesAgainstBaseUrl() {
        val html = """
            <html><body>
            <article><p>$longBody</p><img src="/shots/a.jpg" alt="shot"></article>
            </body></html>
        """.trimIndent()
        val markdown = ArticleExtractor.markdown(html, "https://example.com/post")
        assertNotNull(markdown)
        assertTrue(markdown!!.contains("![shot](https://example.com/shots/a.jpg)"))
    }
}
