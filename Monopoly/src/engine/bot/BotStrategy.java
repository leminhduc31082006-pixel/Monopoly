package engine.bot;

import engine.GameEngine;
import model.Player;

public interface BotStrategy {
    /** Chơi nốt lượt hiện tại cho player bằng cách gọi lại các command của engine. */
    void playTurn(GameEngine engine, Player player);
}
