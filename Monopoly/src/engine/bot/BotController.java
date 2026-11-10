package engine.bot;

import model.GameRoom;
import model.Player;
import model.RoomStatus;
import engine.GameEngine;

import java.util.OptionalInt;

/**
 * Bot: tự gieo xúc xắc, nộp bảo lãnh khi ở tù nếu dư dả, mua đất khi số dư >= 3 lần giá,
 * rồi kết thúc lượt. Luôn đi qua các command của engine nên chịu cùng validate như người chơi thật.
 * Chỉ được gọi từ bên trong engine (khi engine đang giữ lock).
 */
public class BotController implements BotStrategy {
    public static final int RICH_MULTIPLIER = 3;
    private static final int MAX_STEPS = 8;     // đủ cho tối đa 3 lần gieo (đổ đôi) trong một lượt

    @Override
    public void playTurn(GameEngine engine, Player player) {
        String id = player.getId();
        GameRoom room = engine.getRoom();
        int turnId = room.getTurnId();

        for (int i = 0; i < MAX_STEPS; i++) {
            if (room.getTurnId() != turnId || room.getStatus() != RoomStatus.PLAYING) return;

            switch (engine.getCurrentPhase()) {
                case AWAITING_ROLL -> {
                    if (player.isInJail() && player.getMoney() >= RICH_MULTIPLIER * GameEngine.BAIL_AMOUNT) {
                        engine.payBail(id, turnId);
                    }
                    if (!engine.rollDice(id, turnId)) return;
                }
                case AWAITING_DECISION -> {
                    OptionalInt price = engine.buyablePrice();
                    if (price.isPresent() && player.getMoney() >= RICH_MULTIPLIER * price.getAsInt()) {
                        engine.buyProperty(id, turnId, player.getPosition());
                    }
                    if (!engine.endTurn(id, turnId)) return;
                }
                default -> { return; }
            }
        }
    }
}
