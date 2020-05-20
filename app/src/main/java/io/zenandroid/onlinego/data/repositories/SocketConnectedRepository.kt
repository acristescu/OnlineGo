package io.zenandroid.onlinego.data.repositories

interface SocketConnectedRepository {
    fun onSocketConnected()
    fun onSocketDisconnected()
}