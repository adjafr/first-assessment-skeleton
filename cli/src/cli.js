import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let mostRecentCommand

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [server] = "localhost"') //AJ added [server] to give the optional command to load a server
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.server, port: 8080 }, () => {  //aj replaced localhost with args.server to connect to server of choosing
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })



  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')

    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      mostRecentCommand = command

    } else if (command === 'broadcast') { //added by AJ
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ
      mostRecentCommand = command

    } else if (command === '@user') { //added by AJ
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ
      mostRecentCommand = command

    } else if (command === 'users') { //added by AJ
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ
      mostRecentCommand = command

    // } else if (command === '') {
    //   command = mostRecentCommand
    //   server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ


    } else {
      this.log(`Command <${command}> was not recognized`)
    }



    callback()
  })
