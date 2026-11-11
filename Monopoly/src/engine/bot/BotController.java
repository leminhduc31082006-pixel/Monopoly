package engine.bot;

import engine.GameEngine;
import engine.GameEngine.ActionResult;
import engine.TurnPhase;

import model.GameRoom;
import model.Player;
import model.PlayerStatus;
import model.tile.PropertyTile;
import model.tile.Tile;

import java.util.UUID;

/**
 * Controller tự động cho người chơi bot
 * hoặc khi người chơi bị timeout.
 */
public class BotController {

    public static void processBotTurn(GameEngine engine) {

        if (engine == null) {
            return;
        }

        GameRoom room = engine.getRoom();

        Player player = room.getCurrentPlayer();

        if (player == null) {
            return;
        }

        processBotTurn(engine, player.getId(), room.getTurnId());
    }

    /**
     * Xử lý đúng người chơi và lượt đã timeout.
     * Phương thức này được TurnTimerService gọi.
     */
    public static void processBotTurn(GameEngine engine,
                                      String playerId,
                                      int turnId) {

        if (engine == null || playerId == null) {
            return;
        }

        GameRoom room = engine.getRoom();
        String roomId = room.getId();

        while (engine.isBotTurnActive(roomId, playerId, turnId)) {

            Player player = room.getCurrentPlayer();

            if (player == null || !player.getId().equals(playerId)) {
                return;
            }

            TurnPhase phase = engine.getBotActionPhase(
                    roomId, playerId, turnId
            );

            // 1. Nếu người chơi đang ở tù thì bỏ lượt.
            if (phase == TurnPhase.AWAITING_ROLL
                    && player.getStatus() == PlayerStatus.IN_JAIL) {

                engine.botEndTurn(
                        roomId, playerId, turnId, newId("skip")
                );

                return;
            }

            // 2. Tự động gieo xúc xắc.
            if (phase == TurnPhase.AWAITING_ROLL) {

                ActionResult result = engine.botRollDice(
                        roomId, playerId, turnId, newId("roll")
                );

                if ("THREE_DOUBLES".equals(result.getCode())
                        || "BANKRUPT".equals(result.getCode())
                        || "GAME_FINISHED".equals(result.getCode())) {
                    return;
                }

                if (!result.isSuccess()) {
                    // Nếu lỗi nhưng engine đã chuyển sang
                    // giai đoạn quyết định thì vẫn hoàn tất lượt.
                    if (engine.getBotActionPhase(
                            roomId, playerId, turnId
                    ) == TurnPhase.AWAITING_DECISION) {
                        continue;
                    }

                    return;
                }

                continue;
            }

            // 3. Quyết định mua đất nếu đủ tiền.
            if (phase == TurnPhase.AWAITING_DECISION) {

                buyPropertyIfPossible(
                        engine, player, roomId, playerId, turnId
                );

                // 4. Kết thúc lượt.
                ActionResult result = engine.botEndTurn(
                        roomId, playerId, turnId, newId("end")
                );

                if (!result.isSuccess()) {
                    return;
                }

                // Nếu gieo đôi thì Bot được gieo tiếp.
                if ("EXTRA_ROLL".equals(result.getCode())) {
                    continue;
                }

                return;
            }

            // Không xử lý nếu phase không hợp lệ.
            return;
        }
    }

    /**
     * Bot mua đất khi tiền >= 3 lần giá mua.
     */
    private static void buyPropertyIfPossible(
            GameEngine engine,
            Player player,
            String roomId,
            String playerId,
            int turnId) {

        Tile tile = engine.getRoom()
                .getBoard()
                .getTile(player.getPosition());

        if (!(tile instanceof PropertyTile)) {
            return;
        }

        PropertyTile property = (PropertyTile) tile;

        long requiredMoney = 3L * property.getPrice();

        if (!property.hasOwner()
                && player.getMoney() >= requiredMoney) {

            engine.botBuyProperty(
                    roomId,
                    playerId,
                    turnId,
                    newId("buy")
            );
        }
    }

    // Tạo mã riêng cho mỗi hành động của Bot.
    private static String newId(String action) {
        return "BOT-" + action + "-" + UUID.randomUUID();
    }
}
