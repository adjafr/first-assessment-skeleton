export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents, targetUser }) {
    this.username = username
    this.command = command
    this.contents = contents
    this.targetUser = targetUser
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      targetUser: this.targetUser
    })
  }

  toString () {
    return this.contents
  }
}
