package com.spotify.heroic.metrics.async;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import com.spotify.heroic.async.Callback;
import com.spotify.heroic.async.CancelReason;
import com.spotify.heroic.model.WriteResponse;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MergeWriteResponse implements
        Callback.Reducer<WriteResponse, WriteResponse> {
    private static final MergeWriteResponse instance = new MergeWriteResponse();

    public static MergeWriteResponse get() {
        return instance;
    }

    @Override
    public WriteResponse resolved(Collection<WriteResponse> results,
            Collection<Exception> errors, Collection<CancelReason> cancelled)
            throws Exception {
        WriteResponse response = new WriteResponse(0, errors, cancelled);

        for (final WriteResponse result : results) {
            response = response.merge(result);
        }

        return response;
    }
}