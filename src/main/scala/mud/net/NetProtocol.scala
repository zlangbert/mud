package mud.net

import akka.util.ByteString

object NetProtocol {

  case class Send(data: ByteString)
}
