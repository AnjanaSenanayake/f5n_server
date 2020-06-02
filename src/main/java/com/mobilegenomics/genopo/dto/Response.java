package com.mobilegenomics.genopo.dto;

// Response class
public class Response<U, V>
{
    public final U status;
    public final V message;

    // Constructs a new Pair with specified values
    public Response(U status, V message)
    {
        this.status = status;
        this.message = message;
    }

    @Override
    // Checks specified object is "equal to" current object or not
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Response<?, ?> pair = (Response<?, ?>) o;

        // call equals() method of the underlying objects
        if (!status.equals(pair.status))
            return false;
        return message.equals(pair.message);
    }

    @Override
    // Computes hash code for an object to support hash tables
    public int hashCode()
    {
        // use hash codes of the underlying objects
        return 31 * status.hashCode() + message.hashCode();
    }

    @Override
    public String toString()
    {
        return "(" + status + ", " + message + ")";
    }

    // Factory method for creating a Typed Pair immutable instance
    public static <U, V> Response <U, V> of(U a, V b)
    {
        // calls private constructor
        return new Response<>(a, b);
    }
}