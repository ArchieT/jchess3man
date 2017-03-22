package pl.edu.platinum.archiet.jchess3man.engine;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Michał Krzysztof Feiler on 03.02.17.
 */
public class GameState {
    public final Board board;
    public final MoatsState moatsState;
    public final Color movesNext;
    public final CastlingPossibilities castlingPossibilities;
    public final EnPassantStore enPassantStore;
    public final int halfMoveClock;
    public final int fullMoveNumber;
    public final PlayersAlive alivePlayers;

    public GameState(
            Board board,
            MoatsState moatsState,
            Color movesNext,
            CastlingPossibilities castlingPossibilities,
            EnPassantStore enPassantStore,
            int halfMoveClock,
            int fullMoveNumber,
            PlayersAlive alivePlayers) {
        this.board = board;
        this.moatsState = moatsState;
        this.movesNext = movesNext;
        this.castlingPossibilities = castlingPossibilities;
        this.enPassantStore = enPassantStore;
        this.halfMoveClock = halfMoveClock;
        this.fullMoveNumber = fullMoveNumber;
        this.alivePlayers = alivePlayers;
    }

    public GameState(GameState source) {
        this(
                (source.board instanceof MutableBoard)
                        ? source.board.mutableCopy()
                        : source.board,
                source.moatsState,
                source.movesNext,
                source.castlingPossibilities,
                source.enPassantStore,
                source.halfMoveClock,
                source.fullMoveNumber,
                source.alivePlayers
        );
    }

    public static final GameState newGame = new GameState(
            NewGameBoardImpl.c,
            MoatsState.noBridges,
            Color.White,
            CastlingPossibilities.all,
            EnPassantStore.empty,
            0, 0,
            PlayersAlive.all
    );
    public GameState(GameState source,
                     @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                             Optional<PlayersAlive> withPlayersAlive
    ) {
        this(source, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), withPlayersAlive);
    }

    public GameState(GameState source, PlayersAlive withPlayersAlive) {
        this(source, Optional.of(withPlayersAlive));
    }

    public GameState(GameState source,
                     @Nullable Board withBoard,
                     @Nullable MoatsState withMoatsState,
                     @Nullable Color withMovesNext,
                     @Nullable CastlingPossibilities withCastlingPossibilities,
                     @Nullable EnPassantStore withEnPassantStore,
                     @Nullable Integer withHalfMoveClock,
                     @Nullable Integer withFullMoveNumber,
                     @Nullable PlayersAlive withPlayersAlive
    ) {
        this(
                source,
                Optional.ofNullable(withBoard),
                Optional.ofNullable(withMoatsState),
                Optional.ofNullable(withMovesNext),
                Optional.ofNullable(withCastlingPossibilities),
                Optional.ofNullable(withEnPassantStore),
                Optional.ofNullable(withHalfMoveClock),
                Optional.ofNullable(withFullMoveNumber),
                Optional.ofNullable(withPlayersAlive)
        );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public GameState(GameState source,
                     Optional<Board> withBoard,
                     Optional<MoatsState> withMoatsState,
                     Optional<Color> withMovesNext,
                     Optional<CastlingPossibilities> withCastlingPossibilities,
                     Optional<EnPassantStore> withEnPassantStore,
                     Optional<Integer> withHalfMoveClock,
                     Optional<Integer> withFullMoveNumber,
                     Optional<PlayersAlive> withPlayersAlive
    ) {
        this(
                withBoard.isPresent() ? withBoard.get() : (
                        (source.board instanceof MutableBoard) ? source.board.mutableCopy() : source.board),
                withMoatsState.isPresent() ? withMoatsState.get() : source.moatsState,
                withMovesNext.isPresent() ? withMovesNext.get() : source.movesNext,
                withCastlingPossibilities.isPresent() ? withCastlingPossibilities.get() : source.castlingPossibilities,
                withEnPassantStore.isPresent() ? withEnPassantStore.get() : source.enPassantStore,
                withHalfMoveClock.isPresent() ? withHalfMoveClock.get() : source.halfMoveClock,
                withFullMoveNumber.isPresent() ? withFullMoveNumber.get() : source.fullMoveNumber,
                withPlayersAlive.isPresent() ? withPlayersAlive.get() : source.alivePlayers
        );
    }

    public Stream<Pos> amIinCheck(Color who) {
        return board.checkChecking(who, alivePlayers);
    }

    public Stream<Pos> amIinCheck() {
        return amIinCheck(movesNext);
    }

    @Contract(pure = true)
    public boolean _canIMoveWOCheck(Color who) {
        for (final Pos from : new AllPosIterable())
            //noinspection ConstantConditions
            if (!board.isEmpty(from) && board.get(from).color == who)
                for (final Pos to : AMFT.getIterableFor(from))
                    //noinspection ConstantConditions
                    for (final Vector vec : board.get(from).vecs(from, to))
                        try {
                            final VecMove m = new VecMove(vec, from, this);
                            try {
                                //noinspection ResultOfMethodCallIgnored
                                m.afterWOEvaluatingDeath();
                            } catch (IllegalMoveException ignored) {
                                return true;
                            }
                        } catch (VectorAdditionFailedException e) {
                            e.printStackTrace();
                            throw new AssertionError(e);
                        } catch (NeedsToBePromotedException ignored) {
                        }
        return false;
    }

    public boolean canIMoveWOCheck(Color who, boolean mutabilitySafe) {
        if (mutabilitySafe && board instanceof MutableBoard) {
            return new GameState(this)._canIMoveWOCheck(who);
        }
        return _canIMoveWOCheck(who);
    }

    public PlayersAlive evalDeath() {
        boolean testCheckmate = true;
        Color player = movesNext;
        PlayersAlive pa = alivePlayers;
        for (int proceduralIndex = 0; proceduralIndex < 3; proceduralIndex++) {
            if (pa.get(player))
                if (testCheckmate)
                    if (!board.whereIsKing(player).isPresent())
                        pa = pa.die(player);
                    else if (!_canIMoveWOCheck(player))
                        pa = pa.die(player);
                    else {
                        testCheckmate = false;
                    }
                else if (!board.whereIsKing(player).isPresent())
                    pa = pa.die(player);
            player = player.next();
        }
        return pa;
    }

    public GameState evaluateDeathThrowingCheck(Color whatColor) throws WeInCheckException {
        return VecMove.evaluateDeathThrowingCheck(this, whatColor);
    }

    public void throwCheck(Color whatColor) throws WeInCheckException {
        VecMove.throwCheck(this, whatColor);
    }

    public GameState evaluateDeath() {
        return VecMove.evaluateDeath(this);
    }

    private Seq<DescMove> genDescMoves(Pos from, Pos to) {
        DescMove move = new DescMove(from, to, this);
        Stream<DescMove.EitherStateOrIllMoveExcept> afters;
        try {
            afters = move.generateAftersWOEvaluatingDeath();
        } catch (NeedsToBePromotedException e) {
            move = new DescMove(from, to, this, FigType.Queen);
            try {
                afters = move.generateAftersWOEvaluatingDeath();
            } catch (NeedsToBePromotedException e1) {
                e1.printStackTrace();
                throw new AssertionError(e1);
            }
        }
        Optional<GameState> any = afters
                .flatMap(DescMove.EitherStateOrIllMoveExcept::flatMapState)
                .findAny();
        if (any.isPresent()) {
            /*
            if (move.pawnPromotion == null)
                //return Seq.of(new Desc(from, to));
                return Seq.of(move);
            else return
                    Seq.of(FigType.Queen, FigType.Rook, FigType.Bishop, FigType.Knight)
                            .map(prom -> new Desc(from, to, prom));
                            */
            return move.promPossible();
        } else return Seq.empty();
    }

    public Seq<DescMove> genDescMoves() {
        Seq<Pos> ours = board.friendsAndOthers(movesNext, alivePlayers).v1.parallel();
        return ours.flatMap(from -> Seq.seq(AMFT.getIterableFor(from)).parallel()
                .flatMap(to -> genDescMoves(from, to)));
    }

    public Seq<GameState> genASAOM(Color ourColor) {
        if (!alivePlayers.get(ourColor) || movesNext.equals(ourColor))
            return Seq.of(this);
        else
            return genDescMoves().flatMap(tft -> genASAOM(ourColor, tft));
    }

    private Seq<GameState> genASAOM(Color ourColor, Desc only) {
        DescMove moveToApply = new DescMove(
                only.from, only.to, this, only.pawnPromotion);
        try {
            Optional<GameState> any = moveToApply.generateAfters()
                    .flatMap(DescMove.EitherStateOrIllMoveExcept::flatMapState)
                    .findAny();
            assert (any.isPresent());
            GameState aft = any.get();
            if (!aft.alivePlayers.get(ourColor) || aft.movesNext.equals(ourColor))
                return Seq.of(aft);
            else return aft.genDescMoves().map(this::genASAOMinternal);
        } catch (NeedsToBePromotedException e) {
            e.printStackTrace();
            throw new AssertionError(e);
        }
    }

    @NotNull
    private GameState genASAOMinternal(DescMove moveToBe) {
        //DescMove moveToBe = new DescMove(
        //        tft.from, tft.to, this, tft.pawnPromotion);
        try {
            Optional<GameState> newAny = moveToBe.generateAfters()
                    .flatMap(DescMove.EitherStateOrIllMoveExcept::flatMapState)
                    .findAny();
            assert newAny.isPresent();
            return newAny.get();
        } catch (NeedsToBePromotedException e) {
            e.printStackTrace();
            throw new AssertionError(e);
        }
    }
}
