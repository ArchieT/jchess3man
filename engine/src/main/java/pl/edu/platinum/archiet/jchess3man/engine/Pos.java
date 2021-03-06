package pl.edu.platinum.archiet.jchess3man.engine;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;

import static pl.edu.platinum.archiet.jchess3man.engine.CastlingVector.kfm;

/**
 * Created by Michał Krzysztof Feiler on 24.01.17.
 */
public class Pos {
    //public final byte rank;
    //public final byte file;
    public final int rank;
    public final int file;

    //public Pos(byte rank, byte file) {
    public Pos(int rank, int file) {
        this.rank = rank;
        this.file = file;
    }

    public Pos(short rank, short file) {
        this((int) rank, (int) file);
    }

    public Pos(byte rank, byte file) {
        this((int) rank, (int) file);
    }

    public Pos(Color color, int rank, int colorFile) {
        this(rank, (color.segm() << 3) + colorFile);
    }

    public static final Pos zero = new Pos(0, 0);

    public Pos addVec(Vector vec) throws VectorAdditionFailedException {
        return vec.addTo(this);
    }

    public String toString() {
        return "[" + rank + "," + file + "]";
    }

    public Color colorSegm() {
        return colorSegm(file);
    }

    @Contract(pure = true)
    public static Color colorSegm(int file) {
        return Color.colors[file / 8];
    }

    public Pos next() {
        return (rank == 5 && file == 23) ? null : new Pos(file == 23 ? rank + 1 : rank, file == 23 ? 0 : file + 1);
    }

    public boolean sameRank(Pos ano) {
        return rank == ano.rank;
    }

    public boolean sameFile(Pos ano) {
        return file == ano.file;
    }

    public boolean equals(Pos ano) {
        return sameFile(ano) && sameRank(ano);
    }

    public int toInt() {
        return rank * 24 + file;
    }

    public static @Nullable Fig getNewGame(int rank, int file) {
        if (rank == 1) return new Fig.Pawn(Color.fromSegm(file / 8));
        if (rank == 0) {
            final Color theColor = Color.fromSegm(file / 8);
            switch (file % 8) {
                case 0:
                case 7:
                    return new Fig.Rook(theColor);
                case 1:
                case 6:
                    return new Fig.Knight(theColor);
                case 2:
                case 5:
                    return new Fig.Bishop(theColor);
                case 3:
                    return new Fig.Queen(theColor);
                case 4:
                    return new Fig.King(theColor);
                default:
                    throw new IllegalArgumentException(file + " ");
            }
        }
        return null;
    }


    @NotNull
    @Contract(pure = true, value = "null -> fail")
    public static Pos newGameKingPos(@NotNull Color who) {
        return new Pos(0, kfm + (who.segm() << 3));
    }

    public static @Nullable Fig getNewGame(Pos pos) {
        return getNewGame(pos.rank, pos.file);
    }

    public @Nullable Fig getNewGame() {
        return getNewGame(rank, file);
    }

    @Contract(pure = true)
    public static boolean emptyOnNewGame(int rank) {
        return rank > 1;
    }

    @Contract(pure = true)
    public static boolean emptyOnNewGame(Pos pos) {
        return emptyOnNewGame(pos.rank);
    }

    public boolean emptyOnNewGame() {
        return emptyOnNewGame(this);
    }

    @Override
    public int hashCode() {
        assert (file < 24);
        //return rank<<5 | file;
        return toInt();
    }

    public boolean equals(Object obj) {
        return obj instanceof Pos && equals((Pos) obj);
    }

    public boolean isAdjacentFile(Pos ano) {
        return file + 12 % 24 == ano.file;
    }

    public boolean isSameOrAdjacentFile(Pos ano) {
        //return file + 12 % 24 == ano.file % 12;
        return file % 12 == ano.file % 12;
    }

    public CanIDiagonal canIDiagonalTo(Pos ano) {
        return new CanIDiagonal(this, ano);
    }

    public boolean diagonalSomehow(Pos ano) {
        return canIDiagonalTo(ano).toBool();
    }

    public KnightVector knightVectorTo(Pos ano)
            throws CannotConstructVectorException {
        return KnightVector.knightVector(this, ano);
    }

    Optional<KnightVector> optionalKnightVectorTo(Pos ano) {
        try {
            return Optional.of(knightVectorTo(ano));
        } catch (CannotConstructVectorException ignored) {
            return Optional.empty();
        }
    }

    public RankVector rankVectorTo(Pos ano) throws CannotConstructVectorException {
        if (isSameOrAdjacentFile(ano))
            return new RankVector(
                    sameFile(ano) ? ano.rank - rank : 11 - (rank + ano.rank));
        throw new CannotConstructVectorException(this, ano);
    }

    public FileVector fileVectorTo(Pos ano)
            throws CannotConstructVectorException {
        if (sameRank(ano) && !sameFile(ano)) {
            return new FileVector(wrappedFileVector(file, ano.file));
        }
        throw new CannotConstructVectorException(this, ano);
    }

    //* @noinspection SameParameterValue
    public FileVector fileVectorTo(Pos ano, boolean wLong)
            throws CannotConstructVectorException {
        if (sameRank(ano) && !sameFile(ano)) {
            return new FileVector(wrappedFileVector(file, ano.file, wLong));
        }
        throw new CannotConstructVectorException(this, ano);
    }

    public FileVector fileVectorLongTo(Pos ano)
            throws CannotConstructVectorException {
        return fileVectorTo(ano, true);
    }

    public ArrayList<AxisVector> axisVectorsTo(Pos ano) {
        ArrayList<AxisVector> arr = new ArrayList<>();
        try {
            arr.add(rankVectorTo(ano));
        } catch (CannotConstructVectorException ignored) {
        }
        try {
            arr.add(fileVectorTo(ano));
        } catch (CannotConstructVectorException ignored) {
        }
        try {
            arr.add(fileVectorLongTo(ano));
        } catch (CannotConstructVectorException ignored) {
        }
        return arr;
    }

    public FileVector kingFileVectorTo(Pos ano) throws CannotConstructVectorException {
        FileVector tryin = new FileVector(1, true);
        if (tryin.addTo(this).equals(ano)) return tryin;
        tryin = new FileVector(1, false);
        if (tryin.addTo(this).equals(ano)) return tryin;
        throw new CannotConstructVectorException(this, ano);
    }

    public RankVector kingRankVectorTo(Pos ano) throws CannotConstructVectorException {
        RankVector tryin = new RankVector(1, true);
        if (tryin.addTo(this).equals(ano)) return tryin;
        tryin = new RankVector(1, false);
        if (tryin.addTo(this).equals(ano)) return tryin;
        throw new CannotConstructVectorException(this, ano);
    }

    public AxisVector kingAxisVectorTo(Pos ano) throws CannotConstructVectorException {
        try {
            return kingFileVectorTo(ano);
        } catch (CannotConstructVectorException ignored) {
            return kingRankVectorTo(ano);
        }
    }

    public DiagonalVector kingDiagonalVectorTo(Pos ano)
            throws CannotConstructVectorException {
        DiagonalVector theDirect;
        theDirect = new DiagonalVector(1, true, true);
        try {
            if (theDirect.addTo(this).equals(ano))
                return theDirect;
        } catch (VectorAdditionFailedException ignored) {
        }
        theDirect = new DiagonalVector(1, false, false);
        try {
            if (theDirect.addTo(this).equals(ano))
                return theDirect;
        } catch (VectorAdditionFailedException ignored) {
        }
        theDirect = new DiagonalVector(1, true, false);
        try {
            if (theDirect.addTo(this).equals(ano))
                return theDirect;
        } catch (VectorAdditionFailedException ignored) {
        }
        theDirect = new DiagonalVector(1, false, true);
        try {
            if (theDirect.addTo(this).equals(ano))
                return theDirect;
        } catch (VectorAdditionFailedException ignored) {
        }
        throw new CannotConstructVectorException(this, ano);
    }

    public ContinuousVector kingContinuousVectorTo(Pos ano)
            throws CannotConstructVectorException {
        try {
            return kingAxisVectorTo(ano);
        } catch (CannotConstructVectorException ignored) {
            return kingDiagonalVectorTo(ano);
        }
    }

    public CastlingVector castlingVectorTo(Pos ano) throws CannotConstructVectorException {
        return CastlingVector.castlingVector(this, ano);
    }

    public Vector kingVectorTo(Pos ano) throws CannotConstructVectorException {
        try {
            return kingContinuousVectorTo(ano);
        } catch (CannotConstructVectorException ignored) {
            return castlingVectorTo(ano);
        }
    }

    public LinkedList<DiagonalVector> diagonalVectorsTo(Pos ano) {
        LinkedList<DiagonalVector> ret = new LinkedList<>();
        int fileDiff = wrappedFileVector(file, ano.file);
        boolean plusFile = fileDiff > 0;
        int absFileDiff = plusFile ? fileDiff : -fileDiff;
        boolean inwardShort = ano.rank > rank;
        int absRankDiff = (inwardShort ? ano.rank - rank : rank - ano.rank);
        //if the move is not to the same rank
        if (rank != ano.rank && absFileDiff == absRankDiff)
            ret.add(new DiagonalVector(absFileDiff, inwardShort, plusFile));
        int rankSum = ano.rank + rank;
        if (absFileDiff != 0 && absFileDiff == rankSum)
            ret.add(new DiagonalVector(
                    5 + 5 + 1 - rankSum /*(5-s)+1+(5-r)*/, true, !plusFile));
        return ret;
    }

    public ArrayList<ContinuousVector> continuousVectorsTo(Pos ano) {
        ArrayList<ContinuousVector> ret = new ArrayList<>();
        ret.addAll(axisVectorsTo(ano));
        ret.addAll(diagonalVectorsTo(ano));
        return ret;
    }

    public PawnWalkVector pawnWalkVectorTo(Pos ano) throws CannotConstructVectorException {
        PawnWalkVector tryin = new PawnWalkVector(true);
        if (tryin.addTo(this).equals(ano)) return tryin;
        tryin = new PawnWalkVector(false);
        if (tryin.addTo(this).equals(ano)) return tryin;
        throw new CannotConstructVectorException(this, ano);
    }

    public PawnLongJumpVector pawnLongJumpVectorTo(Pos ano) throws CannotConstructVectorException {
        if (PawnLongJumpVector.willDo(this, ano))
            return new PawnLongJumpVector();
        throw new CannotConstructVectorException(this, ano);
    }

    public PawnCapVector pawnCapVectorTo(Pos ano) throws CannotConstructVectorException {
        return PawnCapVector.pawnCapVector(this, ano);
    }

    public PawnVector pawnVectorTo(Pos ano) throws CannotConstructVectorException {
        try {
            return pawnLongJumpVectorTo(ano);
        } catch (CannotConstructVectorException ignored) {
            try {
                return pawnWalkVectorTo(ano);
            } catch (CannotConstructVectorException ignoredAsWell) {
                return pawnCapVectorTo(ano);
            }
        }
    }

    @Contract(pure = true)
    private static int wrappedFileVector(int from, int to) {
        return wrappedFileVector(from, to, false);
    }

    @Contract(pure = true)
    private static int wrappedFileVector(int from, int to, boolean wLong) {
        int diff = to - from;
        int sgn = diff < 0 ? -1 : 1;
        return ((diff * sgn > 12) == wLong) ? diff : (diff - 24 * sgn);
    }
}
