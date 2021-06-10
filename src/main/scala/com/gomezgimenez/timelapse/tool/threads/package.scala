package com.gomezgimenez.timelapse.tool

import java.awt.image.BufferedImage

package object threads {

  trait TrackerListener {
    def invalidate(t: Tracker): Unit
    def capture(img: BufferedImage): Unit
  }
}
