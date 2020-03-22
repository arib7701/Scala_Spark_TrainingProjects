package coinyser

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.TimeZone

import cats.effect.IO
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.pusher.client.Client
import com.pusher.client.channel.SubscriptionEventListener
import com.typesafe.scalalogging.StrictLogging

object StreamingProducer extends StrictLogging {

  /*
    Pusher is use to deliver stream of message using publish/subscribe pattern
    here subscribe to channel "lives_trades"
    channel binded to callback fct onTradeReceived
    onTradeReceived will be called everytime we receive a new trade
   */

  def subscribe(pusher: Client)(onTradeReceived: String => Unit): IO[Unit] = {
    println("Into subscribe")

    for {
      _ <- IO(pusher.connect())
      channel <- IO(pusher.subscribe("live_trades_ethusd"))

      _ <- IO(channel.bind("data", new SubscriptionEventListener() {
        override def onEvent(channel: String, event: String, data: String): Unit = {
          logger.info(s"Received event: $event with data: $data")
          onTradeReceived(data)
        }
      }))
    } yield ()
  }


  val mapper: ObjectMapper = {
    val m = new ObjectMapper()
    m.registerModule(DefaultScalaModule)
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    m.setDateFormat(sdf)
  }

  def deserializeWebsocketTransaction(s: String): WebsocketTransaction = {
    // use jackson java lib to deserialize json object
    mapper.readValue(s, classOf[WebsocketTransaction])
  }

  def convertWsTransaction(wsTx: WebsocketTransaction): Transaction =
    Transaction(timestamp = new Timestamp(wsTx.timestamp.toLong * 1000), tid = wsTx.id, price = wsTx.price, sell = wsTx.`type` == 1, amount = wsTx.amount)

  def serializeTransaction(tx: Transaction): String =
    mapper.writeValueAsString(tx)
}
