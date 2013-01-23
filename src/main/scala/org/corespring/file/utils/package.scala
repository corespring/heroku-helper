package org.corespring.file

import java.io.{FileWriter, File}

package object utils {

  def write(path: String, contents: String) {
    val fw = new FileWriter(path)
    fw.write(contents)
    fw.close()
    new File(path)
  }

}
