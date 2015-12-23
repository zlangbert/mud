package mud.net

import akka.util.ByteString

object NetProtocol {

  /**
    * Sent to the player when data is received
    * @param data
    */
  case class Received(data: ByteString)

  /**
    * Send to the connection actor to push data to the client
    * @param data
    */
  case class Send(data: ByteString)

  /**
    * An empty send
    */
  val SendEmpty: Send = Send(ByteString())

  def prepareResponse(response: String): Send = {
    val data = ByteString(response + "\n")
    NetProtocol.Send(data)
  }
}
