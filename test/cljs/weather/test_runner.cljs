(ns weather.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [weather.test-core]))

(doo-tests 'weather.test-core)
