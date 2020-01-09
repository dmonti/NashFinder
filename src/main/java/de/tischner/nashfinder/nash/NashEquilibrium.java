package de.tischner.nashfinder.nash;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.tischner.nashfinder.game.StrategicGame;
import de.tischner.nashfinder.game.util.PlayerAction;
import de.tischner.nashfinder.locale.ErrorMessages;
import de.tischner.nashfinder.util.EExpectedUtilty;
import de.tischner.nashfinder.util.MathUtil;
import net.sf.javailp.Result;

/**
 * Object that represents a nash equlibrium in a game. Such nash equilibria
 * specify a {@link de.tischner.nashfinder.nash.NashStrategy NashStrategy} and
 * an expected utility for every player.
 * 
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 *
 * @param <PLAYER>
 *            Class of players
 * @param <ACTION>
 *            Class of actions
 */
public final class NashEquilibrium<PLAYER, ACTION> {

	/**
	 * Decimal scale to round values to.
	 */
	private static final int ROUNDING_DECIMAL_SCALE = 2;

	/**
	 * Creates a nash equilibrium based on the results of a solved <i>linear
	 * program</i> (LP).
	 * 
	 * @param <PLAYER>
	 *            The actual class of the players
	 * @param <ACTION>
	 *            The actual class of actions
	 * @param result
	 *            Results of a solved LP
	 * @param game
	 *            Game the equilibrium and the results belong to
	 * @return The nash equilibrium that was computed by the LP or <tt>null</tt>
	 *         if there is no nash equilibrium
	 */
	public static <PLAYER, ACTION> NashEquilibrium<PLAYER, ACTION> extractFromLcpResults(final Result result,
			final StrategicGame<PLAYER, ACTION> game) {
		// If there is no result, there is no nash equilibrium
		if (result == null) {
			return null;
		}
		final NashEquilibrium<PLAYER, ACTION> nashEquilibrium = new NashEquilibrium<>();

		PLAYER firstPlayer = null;
		PLAYER secondPlayer = null;
		Set<ACTION> firstPlayerActions = null;
		Set<ACTION> secondPlayerActions = null;

		final Iterator<PLAYER> playerIter = game.getPlayers();
		if (playerIter.hasNext()) {
			firstPlayer = playerIter.next();
			if (playerIter.hasNext()) {
				secondPlayer = playerIter.next();

				firstPlayerActions = game.getPlayerActions(firstPlayer);
				secondPlayerActions = game.getPlayerActions(secondPlayer);
			}
		}
		if (firstPlayerActions == null || secondPlayerActions == null || firstPlayer == null || secondPlayer == null) {
			throw new IllegalArgumentException(ErrorMessages.NASH_EQUILIBRIUM_COULD_NOT_EXTRACT_LCP);
		}

		// Extract utility
		nashEquilibrium.setExpectedUtilityForPlayer(firstPlayer,
				MathUtil.roundNumberTo(result.getPrimalValue(EExpectedUtilty.FIRST_PLAYER), ROUNDING_DECIMAL_SCALE));
		nashEquilibrium.setExpectedUtilityForPlayer(secondPlayer,
				MathUtil.roundNumberTo(result.getPrimalValue(EExpectedUtilty.SECOND_PLAYER), ROUNDING_DECIMAL_SCALE));

		// Extract nash strategies
		final NashStrategy<ACTION> firstPlayerStrategy = extractPlayerNashStrategyFromLcpResults(result, firstPlayer,
				firstPlayerActions);
		nashEquilibrium.setNashStrategyForPlayer(firstPlayer, firstPlayerStrategy);
		final NashStrategy<ACTION> secondPlayerStrategy = extractPlayerNashStrategyFromLcpResults(result, secondPlayer,
				secondPlayerActions);
		nashEquilibrium.setNashStrategyForPlayer(secondPlayer, secondPlayerStrategy);

		return nashEquilibrium;
	}

	/**
	 * Creates a nash strategy for a given player based on the results of a
	 * solved <i>linear program</i> (LP).
	 * 
	 * @param <PLAYER>
	 *            The actual class of players
	 * @param <ACTION>
	 *            The actual class of actions
	 * @param result
	 *            Results of a solved LP
	 * @param player
	 *            Player to extract a nash strategy for
	 * @param playerActions
	 *            Actions that belong to the given player in the given game
	 * @return A nash strategy for the given player that was computed by the LP
	 */
	private static <PLAYER, ACTION> NashStrategy<ACTION> extractPlayerNashStrategyFromLcpResults(final Result result,
			final PLAYER player, final Set<ACTION> playerActions) {
		final NashStrategy<ACTION> nashStrategy = new NashStrategy<>();
		for (final ACTION action : playerActions) {
			final PlayerAction<PLAYER, ACTION> playerAction = new PlayerAction<>(player, action);
			final Number probability = result.getPrimalValue(playerAction);
			if (probability != null) {
				nashStrategy.addAction(action, MathUtil.roundNumberTo(probability, ROUNDING_DECIMAL_SCALE));
			}
		}
		return nashStrategy;
	}

	/**
	 * Data structure that allows a fast access to the strategies of a given
	 * player that result in this equilibrium.
	 */
	private final Map<PLAYER, NashStrategy<ACTION>> mPlayerToStrategy;

	/**
	 * Data structure that allows a fast access to the expected utility for a
	 * given player in this nash equilibrium.
	 */
	private final Map<PLAYER, Number> mPlayerToUtility;

	/**
	 * Creates a new empty nash equilibrium.
	 */
	public NashEquilibrium() {
		this.mPlayerToStrategy = new LinkedHashMap<>();
		this.mPlayerToUtility = new LinkedHashMap<>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof NashEquilibrium)) {
			return false;
		}
		final NashEquilibrium<?, ?> other = (NashEquilibrium<?, ?>) obj;
		if (this.mPlayerToStrategy == null) {
			if (other.mPlayerToStrategy != null) {
				return false;
			}
		} else if (!this.mPlayerToStrategy.equals(other.mPlayerToStrategy)) {
			return false;
		}
		if (this.mPlayerToUtility == null) {
			if (other.mPlayerToUtility != null) {
				return false;
			}
		} else if (!this.mPlayerToUtility.equals(other.mPlayerToUtility)) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the expected utility of a given player.
	 * 
	 * @param player
	 *            Player to get the utility for
	 * @return The expected utility of a given player
	 */
	public Number getExpectedUtilityOfPlayer(final PLAYER player) {
		return this.mPlayerToUtility.get(player);
	}

	/**
	 * Gets the nash strategy of a given player that result in this equilibrium.
	 * 
	 * @param player
	 *            Player to get the strategy for
	 * @return The nash strategy of the given player
	 */
	public NashStrategy<ACTION> getNashStrategyOfPlayer(final PLAYER player) {
		return this.mPlayerToStrategy.get(player);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.mPlayerToStrategy == null) ? 0 : this.mPlayerToStrategy.hashCode());
		result = prime * result + ((this.mPlayerToUtility == null) ? 0 : this.mPlayerToUtility.hashCode());
		return result;
	}

	/**
	 * Sets the expected utility for a given player in this nash equilibrium.
	 * 
	 * @param player
	 *            Player to set the utility for
	 * @param expectedUtility
	 *            The expected utility for the given player in this nash
	 *            equilibrium
	 */
	public void setExpectedUtilityForPlayer(final PLAYER player, final Number expectedUtility) {
		this.mPlayerToUtility.put(player, expectedUtility);
	}

	/**
	 * Sets the nash strategy for a given player that belongs to this nash
	 * equilibrium.
	 * 
	 * @param player
	 *            Player to set the strategy for
	 * @param strategy
	 *            Nash strategy for the given player in this nash equilibrium
	 */
	public void setNashStrategyForPlayer(final PLAYER player, final NashStrategy<ACTION> strategy) {
		this.mPlayerToStrategy.put(player, strategy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		final String lineSeparator = System.lineSeparator();

		boolean isFirstEntry = true;
		for (final Entry<PLAYER, NashStrategy<ACTION>> entry : this.mPlayerToStrategy.entrySet()) {
			final Number utility = this.mPlayerToUtility.get(entry.getKey());
			if (isFirstEntry) {
				isFirstEntry = false;
			} else {
				result.append(lineSeparator);
			}
			result.append("\t" + entry.getKey() + ": " + utility + " " + entry.getValue());
		}

		return result.toString();
	}
}
