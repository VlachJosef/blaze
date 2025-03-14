/*
 * Copyright 2014 http4s.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.http4s.blaze.util

import org.specs2.mutable.Specification

class ExecutionSpec extends Specification {
  private def trampoline = Execution.trampoline

  def toRunnable(f: => Unit): Runnable =
    new Runnable {
      override def run(): Unit = f
    }

  def submit(f: => Unit): Unit = trampoline.execute(toRunnable(f))

  "Thread Local Executor" should {
    "submit a working job" in {
      var i = 0

      submit {
        i += 1
      }

      (i must be).equalTo(1)
    }

    "submit multiple working jobs" in {
      var i = 0

      for (_ <- 0 until 10)
        submit {
          i += 1
        }

      (i must be).equalTo(10)
    }

    "submit jobs from within a job" in {
      var i = 0

      submit {
        for (_ <- 0 until 10)
          submit {
            i += 1
          }
      }

      (i must be).equalTo(10)
    }

    "submit a failing job" in {
      var i = 0

      submit {
        sys.error("Boom")
        i += 1
      }

      (i must be).equalTo(0)
    }

    "interleave failing and successful `Runnables`" in {
      var i = 0

      submit {
        for (j <- 0 until 10)
          submit {
            if (j % 2 == 0) submit(i += 1)
            else submit(sys.error("Boom"))
          }
      }

      (i must be).equalTo(5)
    }

    "Not blow the stack" in {
      val iterations = 500000
      var i = 0

      def go(j: Int): Unit =
        submit {
          if (j < iterations) {
            i += 1
            go(j + 1)
          }
        }

      go(0)

      (i must be).equalTo(iterations)
    }
  }
}
