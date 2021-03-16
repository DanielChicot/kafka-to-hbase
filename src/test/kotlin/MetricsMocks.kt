
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import java.util.concurrent.Callable

object MetricsMocks {

    fun runnableTimer(): Summary =
        mock {
            on { time(any<Runnable>()) } doAnswer {
                it.getArgument<Runnable>(0).run()
                10.toDouble()
            }
        }

    fun summary(): Summary =
        mock {
            on { time(any<Callable<*>>()) } doAnswer {
                it.getArgument<Callable<*>>(0).call()
            }
        }

    fun summary(child: Summary.Child): Summary =
        mock {
            on { labels(any()) } doReturn child
        }

    fun counter(child: Counter.Child): Counter =
        mock {
            on { labels(any()) } doReturn child
        }

    fun summaryChild(): Summary.Child =
        mock {
            on { time(any<Callable<*>>()) } doAnswer {
                it.getArgument<Callable<*>>(0).call()
            }

            on { time(any()) } doAnswer {
                it.getArgument<Runnable>(0).run()
                10.toDouble()
            }
        }



}
