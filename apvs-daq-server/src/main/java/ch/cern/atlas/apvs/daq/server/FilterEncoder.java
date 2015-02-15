package ch.cern.atlas.apvs.daq.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.cern.atlas.apvs.domain.Measurement;
import ch.cern.atlas.apvs.domain.Message;
import ch.cern.atlas.apvs.domain.Packet;

public class FilterEncoder extends MessageToMessageEncoder<Packet> {
	private Logger log = LoggerFactory.getLogger(getClass().getName());
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Packet packet,
			List<Object> out) throws Exception {
		
		Packet outPacket = new Packet(packet.getSender(), packet.getReceiver(), packet.getFrameID(), packet.getAcknowledge());
		
		for (Iterator<Message> i=packet.getMessages().iterator(); i.hasNext(); ) {
			Message msg = i.next();
			if (msg.getType().equals("Measurement")) {
				Measurement measurement = (Measurement)msg;
//				log.info(measurement.getSensor());
				//if (measurement.getSensor().equals("DoseRate") ||
				   if (measurement.getSensor().equals("DoseAccum")) {
					outPacket.addMessage(measurement);
				}
			}
		}
		
		if (outPacket.getMessages().size() > 0) {
			out.add(outPacket);
		}
	}
	
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        RecyclableArrayList out = null;
        try {
            if (acceptOutboundMessage(msg)) {
                out = RecyclableArrayList.newInstance();
                Packet packet = (Packet) msg;
                try {
                    encode(ctx, packet, out);
                } finally {
                    ReferenceCountUtil.release(packet);
                }

                if (out.isEmpty()) {
                    out.recycle();
                    out = null;
                }
            } else {
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable t) {
            throw new EncoderException(t);
        } finally {
            if (out != null) {
                final int sizeMinusOne = out.size() - 1;
                if (sizeMinusOne >= 0) {
                    for (int i = 0; i < sizeMinusOne; i ++) {
                        ctx.write(out.get(i));
                    }
                    ctx.write(out.get(sizeMinusOne), promise);
                }
                out.recycle();
            }
        }
    }
}
