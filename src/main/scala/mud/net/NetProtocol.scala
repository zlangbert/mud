package mud.net

import akka.util.ByteString

object NetProtocol {

  val LineEnding = Vector[Byte](13, 10)

  /**
    * Sent to the player when data is received
    * @param data
    */
  case class Received(data: ByteString)

  /**
    * Send to the connection actor to push data to the client
    * @param data
    */
  case class SendToClient(data: ByteString)
  object SendToClient {
    val Empty = SendToClient(ByteString())
  }

  /**
    * Instruct the client to disconnect
    */
  case object Disconnect

  def prepareResponse(response: String): SendToClient = {
    val data = ByteString(response + "\n")
    NetProtocol.SendToClient(data)
  }

  /**
    * Cleans input from client. Removes line endings
    * @param data
    * @return
    */
  def sanitize(data: ByteString): String = {
    if (data.endsWith(LineEnding))
      data.slice(0, data.length - 2).utf8String
    else data.utf8String
  }
}
